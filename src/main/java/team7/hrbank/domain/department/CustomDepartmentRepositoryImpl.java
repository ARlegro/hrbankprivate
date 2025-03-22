package team7.hrbank.domain.department;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import team7.hrbank.domain.department.dto.DepartmentPageContentDTO;
import team7.hrbank.domain.department.dto.DepartmentResponseDTO;
import team7.hrbank.domain.department.dto.DepartmentSearchCondition;

import java.sql.Array;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import static team7.hrbank.domain.department.QDepartment.*;
import static team7.hrbank.domain.employee.QEmployee.employee;


@Slf4j
@RequiredArgsConstructor
public class CustomDepartmentRepositoryImpl implements CustomDepartmentRepository {

    private final JPAQueryFactory queryFactory;

    public DepartmentResponseDTO findPagingAll1(DepartmentSearchCondition condition) {
        // 하나 더 가져오는 이유 : hasNext판단(로직 마지막에 버리고 추가)
        long limitSize = condition.getSize() + 1;

        log.info("condition 인자 확인: {}", condition);

        // 0. 공통으로 필요한 것들
        String sortedFieldName = condition.getSortedField() != null ? condition.getSortedField().toLowerCase().trim() : "establishmentDate";
        String cursorBeforeChange = condition.getCursor();  //(다음 페이지 시작점)
        String sortDirection = condition.getSortDirection() != null ? condition.getSortDirection().toLowerCase().trim() : "asc";
        Long beforeLastId = condition.getIdAfter(); // 이전 페이지 마지막 요소 id

        BooleanExpression fieldWhereCondition = null;
        if (StringUtils.hasText(cursorBeforeChange)) {
            fieldWhereCondition = getConditionByCursor(cursorBeforeChange, sortedFieldName, sortDirection);
        }

        List<DepartmentPageContentDTO> contentDTOList = queryFactory.select(Projections.constructor(DepartmentPageContentDTO.class,
                        department.id,
                        department.name,
                        department.description,
                        department.establishmentDate,
                        employee.count()
                ))
                .from(department)
                .where(fieldWhereCondition, getIdConditionByIdAfter(beforeLastId), nameOrDescriptionLike(condition.getNameOrDescription()))
                .leftJoin(employee).on(department.id.eq(employee.department.id))
                .groupBy(department.id)
                .orderBy(getOrderFieldSpecifier(sortedFieldName, sortDirection), department.id.asc())
                .limit(limitSize).fetch();

        // 이것도 최적화해서 이전에 가져올 수 있을것 같은데...코드가 너무 지저분
        Long totalCount = queryFactory.select(department.count())
                .from(department)
                .where(nameOrDescriptionLike(condition.getNameOrDescription()))
                .fetchOne();



        // hasNext 판단 1. content의 사이즈가 11인 경우
        boolean hasNext = contentDTOList.size() == limitSize;
        String encodedNextCursor = null;
        Integer nextIdAfter = null;
        //if (hasNext && !contentDTOList.isEmpty()) {
        if (hasNext) {
            DepartmentPageContentDTO lastContent = contentDTOList.get(contentDTOList.size() - 1);
            nextIdAfter = Math.toIntExact(lastContent.id());
            encodedNextCursor = sortedFieldName.equalsIgnoreCase("name")
                    ? Base64.getEncoder().encodeToString(lastContent.name().getBytes())
                    : Base64.getEncoder().encodeToString(lastContent.establishmentDate().toString().getBytes());
            contentDTOList.remove(contentDTOList.size() - 1); // 마지막 요소 삭제
        }

        return new DepartmentResponseDTO(
                contentDTOList,
                encodedNextCursor,
                nextIdAfter,
                Math.toIntExact(condition.getSize()),
                totalCount,
                hasNext
        );
    }

    private BooleanExpression getIdConditionByIdAfter(Long idAfter) {
        if (idAfter == null) {
            return null;
        }
        return department.id.gt(idAfter);
    }

    // 커서 (다음 페이지 시작점)
    private BooleanExpression getConditionByCursor(String cursorBeforeChange, String sortedFieldName, String sortDirection) {
        // sortfilename에 따라 cursor스타일이 바뀜

        BooleanExpression cursorCondition = null;
        switch (sortedFieldName) {
            case "name" -> {
                cursorCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.name.loe(cursorBeforeChange)
                        : department.name.goe(cursorBeforeChange);
            }
            case "establishmentDate" -> {
                // 일단 검증먼저
                if (!cursorBeforeChange.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new IllegalArgumentException("유효하지 않은 커서 포맷입니다. : " + cursorBeforeChange);
                }
                LocalDate cursorDate = LocalDate.parse(cursorBeforeChange);
                cursorCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.establishmentDate.loe(cursorDate)
                        : department.establishmentDate.goe(cursorDate);
            }
        }
        return cursorCondition;
    }

    private BooleanExpression nameOrDescriptionLike(String nameOrDescription) {
        //{이름 또는 설명}는 부분 일치 조건입니다.
        if (StringUtils.hasText(nameOrDescription)) {
            return department.name.containsIgnoreCase(nameOrDescription)
                    .or(department.description.containsIgnoreCase(nameOrDescription));
        }
        return null;
    }

    private OrderSpecifier<?> getOrderFieldSpecifier(String field, String direction) {
        String fieldName = field != null ? field.toLowerCase().trim() : "establishmentDate";
        String sortDirection = direction != null ? direction.toLowerCase().trim() : "asc";
        return switch (fieldName) {
            case "name" -> sortDirection.equalsIgnoreCase("desc") ? department.name.desc() : department.name.asc();
            case "establishmentDate" ->
                    sortDirection.equalsIgnoreCase("asc") ? department.establishmentDate.desc() : department.establishmentDate.asc();
            default -> department.establishmentDate.asc();
        };
    }
}
