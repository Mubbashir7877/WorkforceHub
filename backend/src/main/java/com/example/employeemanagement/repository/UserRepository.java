package com.example.employeemanagement.repository;

import com.example.employeemanagement.entity.RoleName;
import com.example.employeemanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByIdAndEmployeeId(Long id, Long employeeId);

    @Query("select case when count(u) > 0 then true else false end from User u join u.roles r where r.name = :roleName")
    boolean existsByRolesContaining(RoleName roleName);

    @Query("select count(u) from User u join u.roles r where r.name = :roleName")
    long countByRolesContaining(RoleName roleName);
}
