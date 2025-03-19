package team7.hrbank.domain.department.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import team7.hrbank.domain.department.dto.DepartmentMapper;
import team7.hrbank.domain.department.dto.PageDepartmentsResponseDto;
import team7.hrbank.domain.department.dto.WithEmployeeCountResponseDto;
import team7.hrbank.domain.department.entity.Department;
import team7.hrbank.domain.department.entity.QDepartment;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


@Repository
@RequiredArgsConstructor
public class CustomDepartmentRepositoryImpl implements CustomDepartmentRepository {

    private final JPAQueryFactory queryFactory;
    private final DepartmentMapper departmentMapper;


    @Override
    public PageDepartmentsResponseDto findDepartments(String nameOrDescription,
                                                              Integer idAfter,
                                                              String cursor,
                                                              Integer size,
                                                              String sortField,
                                                              Sort.Direction sortDirection) {

        QDepartment department = QDepartment.department;
        BooleanBuilder builder = buildSearchCondition(
                nameOrDescription,
                idAfter,
                cursor,
                sortField,
                department);

        //정렬 조건 설정
        JPAQuery<Department> query = queryFactory.selectFrom(department).where(builder);

        //정렬 필드, 방향 설정
        query.orderBy(getOrderSpecifier(sortField, sortDirection,  department));

        // 전체 항목 수 조회
        Long totalCount = getTotalCount(department, nameOrDescription);



        //페이지네이션 처리
        Pageable pageable = PageRequest.of(0, size+1);

        query.limit(pageable.getPageSize()).offset(pageable.getOffset());

        //결과 쿼리 실행
        List<Department> departments = query.fetch();

        //size+1 결과에 따라 다음페이지 존재여부 설정 후, 추가로 받아온 요소 하나 삭제
        boolean hasNext = (departments.size() > size);
        departments = hasNext ? departments.subList(0, size) : departments;

        //필터링한 부서 객체들을 Dto로 변환
        List<WithEmployeeCountResponseDto> newDepartments = departments.stream().map(d->departmentMapper.toDto(d, d.getId())).collect(Collectors.toList());

        // 페이지의 마지막 항목을 가져와서 nextIdAfter와 nextCursor를 설정
        Long nextIdAfter = getNextIdAfter(departments);
        String nextCursor = getNextCursor(departments, sortField);
        PageDepartmentsResponseDto responseDto = new PageDepartmentsResponseDto(newDepartments, nextCursor, nextIdAfter, size, totalCount, hasNext);

        return responseDto;
    }




    //검색된 전체 요소 수 반환
    private Long getTotalCount(QDepartment department, String nameOrDescription) {
        BooleanBuilder builder = new BooleanBuilder();
        if (nameOrDescription != null && !nameOrDescription.trim().isEmpty()) {
            builder.and(department.name.containsIgnoreCase(nameOrDescription)
                    .or(department.description.containsIgnoreCase(nameOrDescription)));
        }
        Long totalCount = queryFactory
                .select(department.count())
                .from(department)
                .where(builder)
                .fetchOne();
        return totalCount;
    }

    //정렬필드에 따라 order 조건 설정
    private OrderSpecifier<?>[] getOrderSpecifier(String sortField, Sort.Direction sortDirection, QDepartment department) {
        OrderSpecifier<?> primarySortSpecifier;
        OrderSpecifier<?> secondarySortSpecifier;
        if ("name".equals(sortField)) {
            primarySortSpecifier = sortDirection.equals(Sort.Direction.ASC)
                    ? department.name.asc()
                    : department.name.desc();
            return new OrderSpecifier[]{primarySortSpecifier};
        } else {
            primarySortSpecifier = sortDirection.equals(Sort.Direction.ASC)
                    ? department.establishedDate.asc()
                    : department.establishedDate.desc();

            secondarySortSpecifier= sortDirection.equals(Sort.Direction.ASC)
                    ? department.id.asc()
                    : department.id.desc();

            return new OrderSpecifier[]{primarySortSpecifier, secondarySortSpecifier};
        }
    }

    //nameOrDescription, idAfter, cursor, sortField 를 고려하여 필터링 조건 빌드
    private BooleanBuilder buildSearchCondition(String nameOrDescription,
                                                Integer idAfter,
                                                String cursor,
                                                String sortField,
                                                QDepartment department) {

        BooleanBuilder builder = new BooleanBuilder();
        //검색어가 공백, null이 아닐 경우 검색어로 필터링
        if (nameOrDescription != null && !nameOrDescription.trim().isEmpty()) {
            builder.and(department.name.containsIgnoreCase(nameOrDescription)
                    .or(department.description.containsIgnoreCase(nameOrDescription)));
        }
        //커서가 null이 아닐 경우, 정렬 기준이 되는 sortField에 따라 cursor의 타입이 정해지고, 그 커서의 값보다 큰 항목만 필터링하도록 함.
        if (cursor != null) {
            String newCursor = new String(Base64.getDecoder().decode(cursor));
            if (sortField.equals("name")) {
                builder.and(department.name.gt(newCursor));
            } else {
                try {
                    LocalDate cursorDate = LocalDate.parse(newCursor);
                    builder.and(department.establishedDate.gt(cursorDate))  // 날짜 기준으로 필터링
                        .or(department.establishedDate.eq(cursorDate)
                                .and(department.id.gt(idAfter))); // 같은 설립일이라면 이전 페이지의 마지막 ID보다 큰 경우만 조회
                } catch (DateTimeParseException e) {
                    // cursor가 유효한 날짜 형식이 아닐 경우 예외 처리
                    throw new IllegalArgumentException("필터링에 필요한 날짜 형식이 올바르지 않습니다.");
                }
            }
        }
        return builder;
    }

    // 커서 String->64바이트 인코딩 메서드
    private String encodeCursor(Department department, String sortField) {
        String cursor = sortField.equals("name")?department.getName():department.getEstablishedDate().toString();
        return Base64.getEncoder().encodeToString(cursor.getBytes());
    }

    //페이지 마지막요소 Id 반환
    private Long getNextIdAfter(List<Department> departments) {
        if (departments.isEmpty()){
            return 0L;
        } else {
            Department lastDepartment = departments.get(departments.size() - 1);
            return lastDepartment.getId();
        }
    }

    //페이지 마지막요소 정렬기준 필드값 64바이트로 반환
    private String getNextCursor(List<Department> departments, String sortField) {
        if (departments.isEmpty()){
            return null;
        } else {
            Department lastDepartment = departments.get(departments.size() - 1);
            return encodeCursor(lastDepartment, sortField);
        }
    }
}
