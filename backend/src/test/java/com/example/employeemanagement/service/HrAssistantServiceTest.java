package com.example.employeemanagement.service;

import com.example.employeemanagement.config.ai.AiProperties;
import com.example.employeemanagement.dto.ChatRequest;
import com.example.employeemanagement.dto.ChatResponse;
import com.example.employeemanagement.dto.ConversationResponse;
import com.example.employeemanagement.entity.*;
import com.example.employeemanagement.exception.AiUnavailableException;
import com.example.employeemanagement.exception.ConversationAccessDeniedException;
import com.example.employeemanagement.exception.ConversationNotFoundException;
import com.example.employeemanagement.repository.AiConversationRepository;
import com.example.employeemanagement.repository.AiMessageRepository;
import com.example.employeemanagement.repository.AiResponseSourceRepository;
import com.example.employeemanagement.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrAssistantServiceTest {

    @Mock private AiConversationRepository conversationRepository;
    @Mock private AiMessageRepository messageRepository;
    @Mock private AiResponseSourceRepository sourceRepository;
    @Mock private HrPolicyKnowledgeService knowledgeService;
    @Mock private ObjectProvider<ChatClient> chatClientProvider;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    private AiProperties aiProperties;
    private HrAssistantService assistantService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setModel("gpt-4o-mini");
        aiProperties.setTopK(5);
        aiProperties.setSimilarityThreshold(0.5);

        assistantService = new HrAssistantService(
                conversationRepository, messageRepository, sourceRepository,
                knowledgeService, chatClientProvider, aiProperties);

        User user = new User("employee@example.com", "hashed");
        user.setId(7L);
        user.setRoles(Set.of(new Role(RoleName.EMPLOYEE)));
        UserPrincipal principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockChatClientChain(String answer) {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(answer);
    }

    @Test
    void chat_whenAiDisabled_throwsAiDisabled() {
        when(chatClientProvider.getIfAvailable()).thenReturn(null);
        ChatRequest request = new ChatRequest();
        request.setMessage("How many vacation days do I get?");

        assertThatThrownBy(() -> assistantService.chat(request))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_DISABLED"));
    }

    @Test
    void chat_newConversation_createsConversationAndPersistsMessages() {
        mockChatClientChain("You may carry over up to 5 unused leave days.");
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> {
            AiMessage m = inv.getArgument(0);
            if (m.getId() == null) m.setId(m.getRole() == AiMessageRole.USER ? 1L : 2L);
            return m;
        });
        when(knowledgeService.isAvailable()).thenReturn(true);

        RetrievedChunk chunk = new RetrievedChunk(5L, "Leave Policy", "LEAVE", "1.0", 4, 0, null,
                "Unused leave carries over up to 5 days.", 0.9);
        when(knowledgeService.retrieveRelevant("How many leave days carry over?", 5, 0.5))
                .thenReturn(List.of(chunk));

        ChatRequest request = new ChatRequest();
        request.setMessage("How many leave days carry over?");

        ChatResponse response = assistantService.chat(request);

        assertThat(response.answer()).contains("carry over up to 5");
        assertThat(response.grounded()).isTrue();
        assertThat(response.conversationId()).isEqualTo(100L);
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).documentId()).isEqualTo(5L);

        verify(sourceRepository).save(any(AiResponseSource.class));
        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getRole()).isEqualTo(AiMessageRole.USER);
        assertThat(messageCaptor.getAllValues().get(1).getRole()).isEqualTo(AiMessageRole.ASSISTANT);
        assertThat(messageCaptor.getAllValues().get(1).getGrounded()).isTrue();
    }

    @Test
    void chat_noRetrievedChunks_isNotGrounded() {
        mockChatClientChain("The available HR policy documents do not provide enough information.");
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(200L);
            return c;
        });
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeService.isAvailable()).thenReturn(false);

        ChatRequest request = new ChatRequest();
        request.setMessage("What is the meaning of life?");

        ChatResponse response = assistantService.chat(request);

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        verify(sourceRepository, never()).save(any());
    }

    @Test
    void chat_existingConversationOwnedByAnotherUser_throwsAccessDenied() {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        AiConversation other = new AiConversation();
        other.setId(50L);
        other.setUserId(999L);
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(other));

        ChatRequest request = new ChatRequest();
        request.setConversationId(50L);
        request.setMessage("Anything?");

        assertThatThrownBy(() -> assistantService.chat(request))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void chat_providerThrows_marksAiRequestFailed() {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("boom"));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(300L);
            return c;
        });
        when(messageRepository.save(any(AiMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeService.isAvailable()).thenReturn(false);

        ChatRequest request = new ChatRequest();
        request.setMessage("Question");

        assertThatThrownBy(() -> assistantService.chat(request))
                .isInstanceOf(AiUnavailableException.class)
                .satisfies(ex -> assertThat(((AiUnavailableException) ex).getErrorCode()).isEqualTo("AI_REQUEST_FAILED"));
    }

    @Test
    void getConversation_missing_throwsNotFound() {
        when(conversationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistantService.getConversation(404L))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void getConversation_ownedByAnotherUser_throwsAccessDenied() {
        AiConversation other = new AiConversation();
        other.setId(9L);
        other.setUserId(999L);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> assistantService.getConversation(9L))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void getConversation_ownedByCurrentUser_returnsMessages() {
        AiConversation conversation = new AiConversation();
        conversation.setId(10L);
        conversation.setUserId(7L);
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        AiMessage userMsg = new AiMessage();
        userMsg.setId(1L);
        userMsg.setConversationId(10L);
        userMsg.setRole(AiMessageRole.USER);
        userMsg.setContent("Question");
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(userMsg));
        when(sourceRepository.findByMessageIdIn(List.of(1L))).thenReturn(List.of());

        ConversationResponse response = assistantService.getConversation(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).role()).isEqualTo("USER");
    }

    @Test
    void deleteConversation_ownedByAnotherUser_throwsAccessDenied() {
        AiConversation other = new AiConversation();
        other.setId(11L);
        other.setUserId(999L);
        when(conversationRepository.findById(11L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> assistantService.deleteConversation(11L))
                .isInstanceOf(ConversationAccessDeniedException.class);

        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void deleteConversation_ownedByCurrentUser_deletesIt() {
        AiConversation conversation = new AiConversation();
        conversation.setId(12L);
        conversation.setUserId(7L);
        when(conversationRepository.findById(12L)).thenReturn(Optional.of(conversation));

        assistantService.deleteConversation(12L);

        verify(conversationRepository).delete(conversation);
    }
}
