package com.example.employeemanagement.controller;

import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the role/ownership matrix enforced by {@code @PreAuthorize} on
 * {@link EmployeeController} and {@link AdminUserController} - end to end,
 * through real JWTs issued by the actual login endpoint, against H2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class EmployeeAuthorizationIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long ownEmployeeId;
    private Long otherEmployeeId;
    private String employeeToken;
    private String managerToken;
    private String hrAdminToken;
    private String systemAdminToken;

    @BeforeEach
    void seed() throws Exception {
        Role employeeRole = saveRole(RoleName.EMPLOYEE);
        Role managerRole = saveRole(RoleName.MANAGER);
        Role hrAdminRole = saveRole(RoleName.HR_ADMIN);
        Role systemAdminRole = saveRole(RoleName.SYSTEM_ADMIN);

        Employee ownEmployee = employeeRepository.save(new Employee(null, "Own", "Employee", "own.employee@example.com"));
        Employee otherEmployee = employeeRepository.save(new Employee(null, "Other", "Employee", "other.employee@example.com"));
        ownEmployeeId = ownEmployee.getId();
        otherEmployeeId = otherEmployee.getId();

        User employeeUser = new User("employee@example.com", passwordEncoder.encode(PASSWORD));
        employeeUser.setRoles(Set.of(employeeRole));
        employeeUser.setEmployee(ownEmployee);
        userRepository.save(employeeUser);

        createUser("manager@example.com", Set.of(managerRole));
        createUser("hradmin@example.com", Set.of(hrAdminRole));
        createUser("sysadmin@example.com", Set.of(systemAdminRole));

        employeeToken = login("employee@example.com");
        managerToken = login("manager@example.com");
        hrAdminToken = login("hradmin@example.com");
        systemAdminToken = login("sysadmin@example.com");
    }

    private Role saveRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private void createUser(String email, Set<Role> roles) {
        User user = new User(email, passwordEncoder.encode(PASSWORD));
        user.setRoles(roles);
        userRepository.save(user);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void listEmployees_employeeRole_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEmployees_managerRole_allowedTemporarily() throws Exception {
        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void listEmployees_hrAdminRole_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + hrAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getOwnProfile_employeeRole_returnsOwnRecord() throws Exception {
        mockMvc.perform(get("/api/v1/employees/me").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("own.employee@example.com"));
    }

    @Test
    void updateOwnProfile_employeeRole_updatesNameFields() throws Exception {
        mockMvc.perform(put("/api/v1/employees/me")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("firstName", "Updated", "lastName", "Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void getEmployeeById_employeeRole_ownId_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + ownEmployeeId).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void getEmployeeById_employeeRole_otherId_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + otherEmployeeId).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEmployee_managerRole_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "firstName", "New", "lastName", "Hire", "email", "new.hire@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEmployee_hrAdminRole_created() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "firstName", "New", "lastName", "Hire", "email", "new.hire@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateEmployee_hrAdminRole_softDeletes() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/" + otherEmployeeId).header("Authorization", "Bearer " + hrAdminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/employees/" + otherEmployeeId).header("Authorization", "Bearer " + hrAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(employeeRepository.findById(otherEmployeeId)).isPresent();
    }

    @Test
    void deactivateEmployee_employeeRole_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/" + otherEmployeeId).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUsersEndpoint_hrAdminRole_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + hrAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUsersEndpoint_systemAdminRole_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void createUser_systemAdminRole_duplicateEmail_conflict() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "employee@example.com",
                                "password", "Password123!",
                                "roles", Set.of("EMPLOYEE")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"));
    }

    @Test
    void updateRoles_systemAdminRole_removingOwnLastAdminRole_conflict() throws Exception {
        User systemAdmin = userRepository.findByEmail("sysadmin@example.com").orElseThrow();

        mockMvc.perform(put("/api/v1/admin/users/" + systemAdmin.getId() + "/roles")
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("roles", Set.of("EMPLOYEE")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LAST_ADMIN_ROLE"));
    }
}
