package team7.hrbank.domain.department;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import team7.hrbank.domain.department.dto.DepartmentPageContentDTO;
import team7.hrbank.domain.department.dto.DepartmentResponseDTO;
import team7.hrbank.domain.department.dto.DepartmentSearchCondition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static team7.hrbank.domain.department.QDepartment.*;
import static team7.hrbank.domain.employee.QEmployee.employee;


// 남은 활용할 것 :     private Integer idAfter; // 이전 페이지 마지막 요소 id
//    private String cursor; // 커서 (다음 페이지 시작점)
// 일단 id : 20 으로 넘어오는거롤 찾기

@RequiredArgsConstructor
public class CustomDepartmentRepositoryImpl implements CustomDepartmentRepository {

    private final JPAQueryFactory queryFactory;

    public DepartmentResponseDTO findPagingAll1(DepartmentSearchCondition condition) {
        // 하나 더 가져오는 이유 : hasNext판단(로직 마지막에 버리고 추가)
        int limitSize = condition.getSize() + 1;

        // 1. sortedField, direction 을 확인하고 orderSpecifier를 만든다.(나중에 뒤에서 sortedFieldName, sortDirection이 필요해서 메서드로 안 만듬)
        String sortedFieldName = condition.getSortedField() != null ? condition.getSortedField().toLowerCase().trim() : "establishmentDate";
        String sortDirection = condition.getSortDirection() != null ? condition.getSortDirection().toLowerCase().trim() : "asc";

        OrderSpecifier<?> orderFieldSpecifier = getOrderFieldSpecifier(sortedFieldName, sortDirection);
        OrderSpecifier<Long> idOrderSpecifier = department.id.asc();

        // 2. query 기본값(getCursor가 null인 상황)
        Long beforeLastId = Long.valueOf(condition.getIdAfter());   // 이전 페이지 마지막 요소 id
        String jsonFormattedStartId = condition.getCursor();  //(다음 페이지 시작점)

        // 상황 1. jsonFormattedStartId가 null인데 beforeLastId가 null인 경우
        // 상황 2. jsonFormattedStartId가 null인데 beforeLastId가 있는 경우
        // 상황 3. jsonFormattedStartId가 있는 경우 : 그냥 jsonFormattedStartId를 활용

        // 상황 1. jsonFormattedStartId가 null인데 beforeLastId가 0이하인 경우
        JPAQuery<DepartmentPageContentDTO> contentQuery = queryFactory.select(Projections.constructor(DepartmentPageContentDTO.class,
                        department.id,
                        department.name,
                        department.description,
                        department.establishmentDate,
                        employee.count()
                ))
                .from(department)
                .where(nameOrDescriptionLike(condition.getNameOrDescription()))
                .join(department, employee.department).on(department.id.eq(employee.department.id))
                .orderBy(orderFieldSpecifier, idOrderSpecifier)
                .limit(limitSize);

        // 상황 2. 커서가 null인데 beforeLastId가 있는 경우
        // id가 잘못됐다면 기본 contentquery를 할지 오류를 보낼지 결정해야
        if (!StringUtils.hasText(jsonFormattedStartId) && (beforeLastId > 0)) {
            // before 활용해서 lastDepartment를 가져온다.
            Department lastDepartment = queryFactory.selectFrom(department)
                    .where(department.id.eq(beforeLastId))
                    .fetchOne();

            // 커서에 맞는 department가 없는 경우 에러 처리
            if (lastDepartment == null) {
                throw new RuntimeException("커서에 맞는 Department를 찾을 수 없습니다: " + beforeLastId);
            }

            String lastName = lastDepartment.getName();
            LocalDate lasEstablishmentDate = lastDepartment.getEstablishmentDate();
            Long lastDepartmentId = lastDepartment.getId();

            BooleanExpression fieldwhereCondition;
            switch (sortedFieldName) {
                case "name" -> fieldwhereCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.name.lt(lastName)
                        : department.name.gt(lastName);
                case "establishmentDate" -> fieldwhereCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.establishmentDate.loe(lasEstablishmentDate).and(department.id.lt(lastDepartmentId))
                        : department.establishmentDate.goe(lasEstablishmentDate).and(department.id.gt(lastDepartmentId));
                default -> fieldwhereCondition = department.establishmentDate.gt(lasEstablishmentDate);
            }
            // 이때 content 추가
            contentQuery = queryFactory.select(Projections.constructor(DepartmentPageContentDTO.class,
                            department.id,
                            department.name,
                            department.description,
                            department.establishmentDate,
                            employee.count())
                    )
                    .from(department)
                    .join(department).on(department.id.eq(employee.department.id))
                    .where(fieldwhereCondition, nameOrDescriptionLike(condition.getNameOrDescription()))
                    .orderBy(orderFieldSpecifier, idOrderSpecifier)
                    .limit(limitSize);
        }


        // 상황 3. cursor를 확인 - null이 아닌 상황 : CURSOR를 기본 베이스로 활용
        if (StringUtils.hasText(jsonFormattedStartId)) {
            // 커서 id 를 활용해서 lastDepartment를 가져온다.
            Department startDepartment = queryFactory.selectFrom(department)
                    .where(getCursorCondition(jsonFormattedStartId))
                    .fetchOne();

            // 커서에 맞는 department가 없는 경우 에러 처리
            if (startDepartment == null) {
                throw new RuntimeException("커서에 맞는 Department를 찾을 수 없습니다: " + jsonFormattedStartId);
            }

            String startDepartmentName = startDepartment.getName();
            LocalDate startEstablishmentDate = startDepartment.getEstablishmentDate();

            BooleanExpression fieldWhereCondition;
            switch (sortedFieldName) {
                case "name" -> fieldWhereCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.name.loe(startDepartmentName)
                        : department.name.goe(startDepartmentName);
                case "establishment" -> fieldWhereCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.establishmentDate.loe(startEstablishmentDate).and(department.id.lt(startDepartment.getId()))
                        : department.establishmentDate.goe(startEstablishmentDate).and(department.id.gt(startDepartment.getId()));
                default -> fieldWhereCondition = department.establishmentDate.goe(startEstablishmentDate);
            }

            // 이때 content 추가
            contentQuery = queryFactory.select(Projections.constructor(DepartmentPageContentDTO.class,
                            department.id,
                            department.name,
                            department.description,
                            department.establishmentDate,
                            employee.count())
                    )
                    .from(department)
                    .join(department).on(department.id.eq(employee.department.id))
                    .where(fieldWhereCondition, nameOrDescriptionLike(condition.getNameOrDescription()))
                    .orderBy(orderFieldSpecifier, idOrderSpecifier)
                    .limit(limitSize);
        }

        List<DepartmentPageContentDTO> contentDTOList = contentQuery.fetch();

        // 이것도 최적화해서 이전에 가져올 수 있을것 같은데...코드가 너무 지저분
        Long totalCount = queryFactory.select(department.count())
                .from(department)
                .where(nameOrDescriptionLike(condition.getNameOrDescription()))
                .fetchOne();

        // hasNext 판단 1. content의 사이즈가 11인 경우
        boolean hasNext = contentDTOList.size() == condition.getSize() + 1;
        String encodedNextCursor = null;
        if (hasNext) {
            encodedNextCursor = Base64.getEncoder().encodeToString(String.format("{\"id\":%d}", contentDTOList.get(condition.getSize()).id()).getBytes());
            contentDTOList.remove(contentDTOList.size() - 1); // 마지막 요소 삭제
            // JSON 형태의 문자열 예: {"id":20}
        }
        Integer idAfter = Math.toIntExact(contentDTOList.get(contentDTOList.size() - 1).id());

        return new DepartmentResponseDTO(
                contentDTOList,
                encodedNextCursor,
                idAfter,
                condition.getSize(),
                totalCount,
                hasNext
        );
    }

    private BooleanExpression getCursorCondition(String cursor) {
        if (cursor != null) {
            String decodedCursor = new String(Base64.getDecoder().decode(cursor));
            Long id = Long.valueOf(decodedCursor.split(":")[1]);
            return department.id.gt(id);
        }
        return null;
    }

    private BooleanExpression nameOrDescriptionLike(String nameOrDescription) {
        //{이름 또는 설명}는 부분 일치 조건입니다.
        if (StringUtils.hasText(nameOrDescription)) {
            return department.name.containsIgnoreCase(nameOrDescription);
        } else if (StringUtils.hasText(nameOrDescription)) {
            return department.description.containsIgnoreCase(nameOrDescription);
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
