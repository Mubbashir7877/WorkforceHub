package com.example.employeemanagement.config;

import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.repository.RoleRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Creates the first SYSTEM_ADMIN account on startup, but only when both
 * INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD are set and no SYSTEM_ADMIN
 * exists yet. A no-op once an administrator exists, so it is safe to leave
 * configured; remove those two environment variables after the first login.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminEmail;
    private final String initialAdminPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.initial-admin.email:}") String initialAdminEmail,
            @Value("${app.initial-admin.password:}") String initialAdminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminEmail = initialAdminEmail;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(initialAdminEmail) || !StringUtils.hasText(initialAdminPassword)) {
            return;
        }

        if (userRepository.existsByRolesContaining(RoleName.SYSTEM_ADMIN)) {
            return;
        }

        String email = CustomUserDetailsService.normalizeEmail(initialAdminEmail);
        if (userRepository.existsByEmail(email)) {
            log.warn("Skipping initial administrator bootstrap: an account already exists for {}", email);
            return;
        }

        Role systemAdminRole = roleRepository.findByName(RoleName.SYSTEM_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "SYSTEM_ADMIN role is missing - check that Flyway migrations have run"));

        User admin = new User(email, passwordEncoder.encode(initialAdminPassword));
        admin.setRoles(Set.of(systemAdminRole));
        userRepository.save(admin);

        log.info("Created initial SYSTEM_ADMIN account for {}. You should unset " +
                "INITIAL_ADMIN_EMAIL/INITIAL_ADMIN_PASSWORD now that it exists.", email);
    }
}
