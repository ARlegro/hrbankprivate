package team7.hrbank.domain.employee.dto;

import com.querydsl.core.util.StringUtils;

import java.time.LocalDate;
import java.util.Objects;

public record EmployeeCreateRequest(
        String name,
        String email,
        Long departmentId,
        String position,
        LocalDate hireDate,
        String memo
) {
    
    // 컴팩트 생성자를 통한 예외처리
    public EmployeeCreateRequest {
        if (StringUtils.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (StringUtils.isNullOrEmpty(email)) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (Objects.isNull(departmentId)) {
            throw new IllegalArgumentException("부서 코드는 필수입니다.");
        }
        if (StringUtils.isNullOrEmpty(position)) {
            throw new IllegalArgumentException("직함은 필수입니다.");
        }
        if (Objects.isNull(hireDate)) {
            throw new IllegalArgumentException("입사일은 필수입니다.");
        }

        name = name.trim();
        email = email.trim();
        position = position.trim();
    }
}