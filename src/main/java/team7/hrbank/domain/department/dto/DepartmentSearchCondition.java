package team7.hrbank.domain.department.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import static lombok.AccessLevel.*;

@AllArgsConstructor
public class DepartmentSearchCondition {
    private static final String DEFAULT_SORTED_FIELD = "establishmentDate";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final Long DEFAULT_SIZE = 10L;

    // 여기다가 조건 및 get메서드 재정의해서 default값 정의
    @Getter private final String nameOrDescription;
    private Integer idAfter; // 이전 페이지 마지막 요소 id
    @Getter private final String cursor; // 커서 (다음 페이지 시작점)

    private final Integer size; // 페이지 사이즈(기본값 10)
    private final String sortedField; // 정렬 필드(name or establishmentDate)
    private final String sortDirection; // 정렬 방향(asc or desc, 기본값은 asc)

    public Long getIdAfter() {
        return idAfter != null ? Long.valueOf(idAfter) : null;
    }

    public Long getSize() {
        return size != null ? Long.valueOf(size) : DEFAULT_SIZE; // 기본값 10
    }

    public String getSortedField() {
        return StringUtils.hasText(sortedField) ? sortedField : DEFAULT_SORTED_FIELD; // 기본값 establishmentDate
    }

    public String getSortDirection() {
        return StringUtils.hasText(sortDirection) ? sortDirection : DEFAULT_SORT_DIRECTION; // 기본값 asc
    }

}
