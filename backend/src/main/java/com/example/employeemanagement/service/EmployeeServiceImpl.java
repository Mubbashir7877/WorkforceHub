package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.mapper.EmployeeMapper;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

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
        return EmployeeMapper.toDto(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto employeeDto) {
        Employee existing = findEmployeeOrThrow(id);

        existing.setFirstName(employeeDto.getFirstName());
        existing.setLastName(employeeDto.getLastName());
        existing.setEmail(employeeDto.getEmail());

        return EmployeeMapper.toDto(employeeRepository.save(existing));
    }

    @Override
    @Transactional
    public void deactivateEmployee(Long id) {
        Employee existing = findEmployeeOrThrow(id);
        existing.setActive(false);
        employeeRepository.save(existing);
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
}
