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
import java.time.LocalDate;
import java.util.List;

import static team7.hrbank.domain.department.QDepartment.*;
import static team7.hrbank.domain.employee.QEmployee.employee;


// 남은 활용할 것 :     private Integer idAfter; // 이전 페이지 마지막 요소 id
//    private String cursor; // 커서 (다음 페이지 시작점)
// 일단 id : 20 으로 넘어오는거롤 찾기
// 상황 2. 커서가 null인데 beforeLastId가 있는 경우 - 그냥 cursor가 잘못되면 null로 할지 아니면 idAftre로 쿼리한번더 나가게하고 찾을지 고민
//        if (!StringUtils.hasText(cursorBeforeChange) && (beforeLastId > 0)) {
//            fieldWhereCondition = getConditionByBeforeLastId(beforeLastId, sortedFieldName, sortDirection);
//        }

@Slf4j
@RequiredArgsConstructor
public class CustomDepartmentRepositoryImpl implements CustomDepartmentRepository {

    private final JPAQueryFactory queryFactory;

    public DepartmentResponseDTO findPagingAll1(DepartmentSearchCondition condition) {
        // 하나 더 가져오는 이유 : hasNext판단(로직 마지막에 버리고 추가)
        int limitSize = condition.getSize() + 1;
        log.info("condition 인자 확인: {}", condition);

        // 0. 공통으로 필요한 것들
        String sortedFieldName = condition.getSortedField() != null ? condition.getSortedField().toLowerCase().trim() : "establishmentDate";
        String cursorBeforeChange = condition.getCursor();  //(다음 페이지 시작점)
        String sortDirection = condition.getSortDirection() != null ? condition.getSortDirection().toLowerCase().trim() : "asc";
        Long beforeLastId = condition.getIdAfter(); // 이전 페이지 마지막 요소 id

        BooleanExpression fieldWhereCondition = null;
        if (StringUtils.hasText(cursorBeforeChange)) {
            fieldWhereCondition = getConditionByBeforeLastId(beforeLastId, cursorBeforeChange, sortedFieldName, sortDirection);
        }
        log.info("fieldWhereCondition 확인: {}", fieldWhereCondition);
        OrderSpecifier<Long> idOrderSpecifier = sortDirection.equalsIgnoreCase("desc")
                ? department.id.desc()
                : department.id.asc();

        List<DepartmentPageContentDTO> contentDTOList = queryFactory.select(Projections.constructor(DepartmentPageContentDTO.class,
                        department.id,
                        department.name,
                        department.description,
                        department.establishmentDate,
                        employee.count()
                ))
                .from(department)
                .where(fieldWhereCondition, nameOrDescriptionLike(condition.getNameOrDescription()))
                .leftJoin(employee).on(department.id.eq(employee.department.id))
                .groupBy(department.id)
                .orderBy(getOrderFieldSpecifier(sortedFieldName, sortDirection), idOrderSpecifier)
                .limit(limitSize).fetch();

        // 이것도 최적화해서 이전에 가져올 수 있을것 같은데...코드가 너무 지저분
        Long totalCount = queryFactory.select(department.count())
                .from(department)
                .where(nameOrDescriptionLike(condition.getNameOrDescription()))
                .fetchOne();

        log.info("ResponseDTOList 로 바꾸기 전 content 점검: {}", contentDTOList);
        log.info("ResponseDTOList 로 바꾸기 전 totalCount 점검: {}", totalCount);

        // hasNext 판단 1. content의 사이즈가 11인 경우
        boolean hasNext = contentDTOList.size() == limitSize;
        String notEncodedNextCursor = null;
        Integer nextIdAfter = null;
        if (hasNext) {
            //DepartmentPageContentDTO nextStartContent = contentDTOList.get(contentDTOList.size() - 1);
            DepartmentPageContentDTO nowLastContent = contentDTOList.get(contentDTOList.size() - 2);
            notEncodedNextCursor = sortedFieldName.equalsIgnoreCase("name")
                    ? nowLastContent.name()
                    : nowLastContent.establishmentDate().toString();
            log.info("notEncodedNextCursor 확인: {}", notEncodedNextCursor);
            nextIdAfter = Math.toIntExact(nowLastContent.id());
            log.info("nextIdAfter 확인: {}", nextIdAfter);
            contentDTOList.remove(contentDTOList.size() - 1); // 마지막 요소 삭제
        }

        return new DepartmentResponseDTO(
                contentDTOList,
                notEncodedNextCursor,
                nextIdAfter,
                condition.getSize(),
                totalCount,
                hasNext
        );
    }

    private BooleanExpression getConditionByBeforeLastId(Long beforeLastId, String cursorBeforeChange, String sortedFieldName, String sortDirection) {
        // idAfter가 null인 경우
        // sortfilename에 따라 cursor스타일이 바뀜
        BooleanExpression cursorCondition = null;
        switch (sortedFieldName) {
            case "name" -> {
                cursorCondition = sortDirection.equalsIgnoreCase("desc")
                        ? department.name.lt(cursorBeforeChange)
                        : department.name.gt(cursorBeforeChange);
                log.info("커서 조건 name 채택");
            }
            case "establishmentdate" -> {
                // 일단 검증먼저
                if (!cursorBeforeChange.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new IllegalArgumentException("유효하지 않은 커서 포맷입니다. : " + cursorBeforeChange);
                }
                LocalDate cursorDate = LocalDate.parse(cursorBeforeChange);
                cursorCondition = sortDirection.equalsIgnoreCase("desc")
                        ? (department.establishmentDate.lt(cursorDate))
                        .or(department.establishmentDate.eq(cursorDate).and(department.id.lt(beforeLastId)))
                        : department.establishmentDate.gt(cursorDate)
                        .or(department.establishmentDate.eq(cursorDate).and(department.id.gt(beforeLastId)));
                log.info("커서 조건 establishmentDate 채택");
            }
        }
        log.info("cursorCondition 확인: {}", cursorCondition);
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
        String fieldName = field != null ? field.toLowerCase().trim() : "establishmentdate";
        String sortDirection = direction != null ? direction.toLowerCase().trim() : "asc";
        return switch (fieldName) {
            case "name" -> sortDirection.equalsIgnoreCase("desc")
                    ? department.name.desc()
                    : department.name.asc();
            case "establishmentdate" -> sortDirection.equalsIgnoreCase("desc")
                    ? department.establishmentDate.desc()
                    : department.establishmentDate.asc();
            default -> department.establishmentDate.asc();
        };
    }
}
