package team7.hrbank.domain.department.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record DepartmentRequestDTO(
        @NotNull(message = "이름 필수")
        String name,
        @NotNull(message = "Description 필수")
        String description,
        @NotNull(message = "개설일은 필수")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate establishmentDate
) {
}
