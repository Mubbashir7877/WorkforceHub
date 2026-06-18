package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.kafka.EmployeeActivityEvent;
import com.example.employeemanagement.kafka.EmployeeActivityEventProducer;
import com.example.employeemanagement.kafka.EmployeeEventType;
import com.example.employeemanagement.mapper.EmployeeMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.UserRepository;
import com.example.employeemanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeActivityEventProducer eventProducer;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(EmployeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(Long id) {
        return EmployeeMapper.toDto(findEmployeeOrThrow(id));
    }

    @Override
    @Transactional
    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        Employee employee = EmployeeMapper.toEntity(employeeDto);
        employee.setId(null);
        employee.setActive(true);
        Employee saved = employeeRepository.save(employee);
        EmployeeDto result = EmployeeMapper.toDto(saved);

        ActorSnapshot actor = captureActor();
        scheduleEvent(EmployeeActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EmployeeEventType.EMPLOYEE_CREATED)
                .entityType("EMPLOYEE")
                .employeeId(saved.getId())
                .employeeEmail(saved.getEmail())
                .employeeFullName(saved.getFirstName() + " " + saved.getLastName())
                .actorUserId(actor.userId())
                .actorEmail(actor.email())
                .actorRoles(actor.roles())
                .message(actor.displayLabel() + " created employee " +
                         saved.getFirstName() + " " + saved.getLastName() + ".")
                .occurredAt(Instant.now())
                .build());

        return result;
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto employeeDto) {
        Employee existing = findEmployeeOrThrow(id);
        existing.setFirstName(employeeDto.getFirstName());
        existing.setLastName(employeeDto.getLastName());
        existing.setEmail(employeeDto.getEmail());
        Employee saved = employeeRepository.save(existing);
        EmployeeDto result = EmployeeMapper.toDto(saved);

        ActorSnapshot actor = captureActor();
        scheduleEvent(EmployeeActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EmployeeEventType.EMPLOYEE_UPDATED)
                .entityType("EMPLOYEE")
                .employeeId(saved.getId())
                .employeeEmail(saved.getEmail())
                .employeeFullName(saved.getFirstName() + " " + saved.getLastName())
                .actorUserId(actor.userId())
                .actorEmail(actor.email())
                .actorRoles(actor.roles())
                .message(actor.displayLabel() + " updated employee " +
                         saved.getFirstName() + " " + saved.getLastName() + ".")
                .occurredAt(Instant.now())
                .build());

        return result;
    }

    @Override
    @Transactional
    public void deactivateEmployee(Long id) {
        Employee existing = findEmployeeOrThrow(id);
        existing.setActive(false);
        employeeRepository.save(existing);

        ActorSnapshot actor = captureActor();
        scheduleEvent(EmployeeActivityEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EmployeeEventType.EMPLOYEE_DEACTIVATED)
                .entityType("EMPLOYEE")
                .employeeId(existing.getId())
                .employeeEmail(existing.getEmail())
                .employeeFullName(existing.getFirstName() + " " + existing.getLastName())
                .actorUserId(actor.userId())
                .actorEmail(actor.email())
                .actorRoles(actor.roles())
                .message(actor.displayLabel() + " deactivated employee " +
                         existing.getFirstName() + " " + existing.getLastName() + ".")
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getOwnEmployee(Long userId) {
        return EmployeeMapper.toDto(findOwnEmployeeOrThrow(userId));
    }

    @Override
    @Transactional
    public EmployeeDto updateOwnEmployee(Long userId, EmployeeSelfUpdateRequest request) {
        Employee employee = findOwnEmployeeOrThrow(userId);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        return EmployeeMapper.toDto(employeeRepository.save(employee));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private Employee findOwnEmployeeOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new ResourceNotFoundException("No employee profile is linked to this account.");
        }
        return employee;
    }

    /**
     * Reads the current authenticated actor from the Spring Security context.
     * Returns a safe "system" placeholder when no principal is available
     * (e.g., in unit tests without a security context).
     */
    private ActorSnapshot captureActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            List<String> roles = principal.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList());
            String label = roles.contains("SYSTEM_ADMIN") ? "System admin" : "HR admin";
            return new ActorSnapshot(principal.getId(), principal.getUsername(), roles,
                    label + " " + principal.getUsername());
        }
        return new ActorSnapshot(null, "system", List.of(), "System");
    }

    /**
     * Registers the event to be published after the current transaction commits.
     * Falls back to immediate publishing when no active transaction is present
     * (e.g., unit test context), so Kafka mocks are exercised correctly.
     */
    private void scheduleEvent(EmployeeActivityEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventProducer.publish(event);
                }
            });
        } else {
            eventProducer.publish(event);
        }
    }

    private record ActorSnapshot(Long userId, String email, List<String> roles, String displayLabel) {}
}
