package team7.hrbank.domain.department.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

public record DepartmentSearchConditionRecord(

        String nameOrDescription,
        Integer idAfter,
        String cursor,
        Integer size,
        @DefaultValue("establishmentDate")
        String sortedField,
        @DefaultValue("asc")
        String sortDirection) {

    public Long size() {
        return idAfter != null ? Long.valueOf(idAfter) : null;
    }
}
