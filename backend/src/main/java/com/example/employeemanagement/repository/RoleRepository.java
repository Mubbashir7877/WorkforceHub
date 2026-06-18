package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.Role;
import com.example.employeemanagement.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
