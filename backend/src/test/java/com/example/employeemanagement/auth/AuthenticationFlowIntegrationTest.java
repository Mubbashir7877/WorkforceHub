package com.example.employeemanagement.auth;

import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real Spring Security filter chain (no mocked beans) against an
 * in-memory H2 database, so it verifies the actual login/refresh/logout wiring
 * and the 401 behavior, not just the orchestration logic in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthenticationFlowIntegrationTest {

    private static final String PASSWORD = "Password123!";

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
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.EMPLOYEE)));

        User enabledUser = new User("active@example.com", passwordEncoder.encode(PASSWORD));
        enabledUser.setRoles(Set.of(employeeRole));
        userRepository.save(enabledUser);

        User disabledUser = new User("disabled@example.com", passwordEncoder.encode(PASSWORD));
        disabledUser.setRoles(Set.of(employeeRole));
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);
    }

    @Test
    void login_validCredentials_returnsTokensAndUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("active@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("active@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("EMPLOYEE"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("active@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_unknownEmail_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nobody@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_disabledAccount_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("disabled@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void protectedEndpoint_withMalformedToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidAccessToken_returnsOk() throws Exception {
        String accessToken = login("active@example.com", PASSWORD).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("active@example.com"));
    }

    @Test
    void refresh_validToken_rotatesAndOldTokenBecomesInvalid() throws Exception {
        String firstRefreshToken = login("active@example.com", PASSWORD).get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertThat(refreshBody.get("refreshToken").asText()).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(firstRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_unknownToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson("totally-unknown-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken_thenRefreshFails() throws Exception {
        String refreshToken = login("active@example.com", PASSWORD).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String loginJson(String email, String password) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return objectMapper.writeValueAsString(body);
    }

    private String refreshJson(String token) throws Exception {
        return objectMapper.writeValueAsString(Map.of("refreshToken", token));
    }
}
