package com.example.employeemanagement.integration;

import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end RAG flow against the real Spring Security filter chain and an
 * in-memory H2 database: upload -> process -> activate -> grounded chat ->
 * conversation ownership isolation -> inactive-document exclusion.
 *
 * The AI provider is stubbed (StubAiConfig below) — this test never makes a
 * real network call to any LLM/embedding API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class HrPolicyAssistantIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String STUB_ANSWER =
            "Based on the provided policy, employees accrue fifteen days of annual leave per year.";

    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void storagePath(DynamicPropertyRegistry registry) {
        registry.add("app.hr-documents.storage-path", () -> storageDir.toString());
    }

    @TestConfiguration
    static class StubAiConfig {

        @Bean
        ChatModel stubChatModel() {
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    return new ChatResponse(List.of(new Generation(new AssistantMessage(STUB_ANSWER))));
                }
            };
        }

        @Bean
        EmbeddingModel stubEmbeddingModel() {
            return new EmbeddingModel() {
                @Override
                public EmbeddingResponse call(EmbeddingRequest request) {
                    List<Embedding> embeddings = new ArrayList<>();
                    int index = 0;
                    for (String text : request.getInstructions()) {
                        embeddings.add(new Embedding(fakeVector(text), index++));
                    }
                    return new EmbeddingResponse(embeddings);
                }

                @Override
                public float[] embed(Document document) {
                    return fakeVector(document.getText());
                }
            };
        }

        @Bean
        ChatClient chatClient(ChatModel stubChatModel) {
            return ChatClient.create(stubChatModel);
        }

        @Bean
        VectorStore vectorStore(EmbeddingModel stubEmbeddingModel) {
            return SimpleVectorStore.builder(stubEmbeddingModel).build();
        }

        private static float[] fakeVector(String text) {
            float[] vector = new float[16];
            String normalized = text == null ? "" : text;
            for (int i = 0; i < normalized.length(); i++) {
                vector[i % 16] += normalized.charAt(i);
            }
            return vector;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        Role hrAdminRole = roleRepository.findByName(RoleName.HR_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.HR_ADMIN)));
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.EMPLOYEE)));

        User hrAdmin = new User("hr.admin@example.com", passwordEncoder.encode(PASSWORD));
        hrAdmin.setRoles(Set.of(hrAdminRole));
        userRepository.save(hrAdmin);

        User employee1 = new User("employee1@example.com", passwordEncoder.encode(PASSWORD));
        employee1.setRoles(Set.of(employeeRole));
        userRepository.save(employee1);

        User employee2 = new User("employee2@example.com", passwordEncoder.encode(PASSWORD));
        employee2.setRoles(Set.of(employeeRole));
        userRepository.save(employee2);
    }

    @Test
    void fullRagFlow_uploadProcessActivateChat_returnsGroundedAnswerWithSources() throws Exception {
        String hrToken = login("hr.admin@example.com");
        String employeeToken = login("employee1@example.com");

        Long documentId = createDraftPolicy(hrToken);
        uploadFile(hrToken, documentId);
        processDocument(hrToken, documentId);
        activateDocument(hrToken, documentId);

        MvcResult chatResult = mockMvc.perform(post("/api/v1/ai/hr-assistant/chat")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "How many vacation days can I carry over?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.answer").value(STUB_ANSWER))
                .andExpect(jsonPath("$.sources[0].documentId").value(documentId))
                .andReturn();

        JsonNode chatBody = objectMapper.readTree(chatResult.getResponse().getContentAsString());
        long conversationId = chatBody.get("conversationId").asLong();

        // The owner can read the conversation back with its messages.
        mockMvc.perform(get("/api/v1/ai/hr-assistant/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));

        // A different employee must not be able to read someone else's conversation.
        String otherEmployeeToken = login("employee2@example.com");
        mockMvc.perform(get("/api/v1/ai/hr-assistant/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + otherEmployeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CONVERSATION_ACCESS_DENIED"));
    }

    @Test
    void deactivatedDocument_isExcludedFromRetrieval_answerIsUngrounded() throws Exception {
        String hrToken = login("hr.admin@example.com");
        String employeeToken = login("employee1@example.com");

        Long documentId = createDraftPolicy(hrToken);
        uploadFile(hrToken, documentId);
        processDocument(hrToken, documentId);
        activateDocument(hrToken, documentId);

        mockMvc.perform(post("/api/v1/hr/policies/" + documentId + "/deactivate")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/ai/hr-assistant/chat")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "message", "How many vacation days can I carry over?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(false))
                .andExpect(jsonPath("$.sources.length()").value(0));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Long createDraftPolicy(String hrToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/hr/policies")
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Leave Policy",
                                "description", "Annual leave rules",
                                "category", "LEAVE",
                                "version", "1.0"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void uploadFile(String hrToken, Long documentId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "leave-policy.txt", "text/plain",
                "Employees accrue fifteen days of annual leave per year. Unused leave carries over up to 5 days."
                        .getBytes());

        mockMvc.perform(multipart("/api/v1/hr/policies/" + documentId + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("UPLOADED"));
    }

    private void processDocument(String hrToken, Long documentId) throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies/" + documentId + "/process")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("READY"));
    }

    private void activateDocument(String hrToken, Long documentId) throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies/" + documentId + "/activate")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
