package team7.hrbank.domain.employee.dto;

public record EmployeeCreateRequest(
        String name,
        String email,
        Long departmentId,
        String position,
        String hireDate,
        String memo
) {
}
