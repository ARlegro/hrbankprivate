package team7.hrbank.domain.employee.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team7.hrbank.domain.employee.entity.Employee;
import team7.hrbank.domain.employee.entity.EmployeeStatus;
import team7.hrbank.domain.employee.entity.QEmployee;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomEmployeeRepositoryImpl implements CustomEmployeeRepository {

    private final JPAQueryFactory queryFactory;
    private final QEmployee qEmployee = QEmployee.employee;

    @Override
    public List<Employee> findEmployees(String nameOrEmail,
                                        String employeeNumber,
                                        String departmentName,
                                        String position,
                                        LocalDate hireDateFrom,
                                        LocalDate hireDateTo,
                                        EmployeeStatus status,
                                        Long idAfter,
                                        String cursor,
                                        int size,
                                        String sortField,
                                        String sortDirection) {

        List<Employee> employees = queryFactory
                .select(qEmployee)
                .from(qEmployee)
                .where(
                        containsNameOrEmail(nameOrEmail),
                        containsEmployeeNumber(employeeNumber),
                        containsPosition(position),
                        betweenHireDate(hireDateFrom, hireDateTo),
                        eqStatus(status),
                        cursorCondition(cursor, sortField, sortDirection, idAfter)
                )
                .orderBy(getSortOrder(sortField, sortDirection))
                .limit(size)
                .fetch();

        return employees;
    }

    @Override
    public long totalCountEmployee(String nameOrEmail, String employeeNumber, String departmentName, String position, LocalDate hireDateFrom, LocalDate hireDateTo, EmployeeStatus status) {

        return queryFactory
                .select(qEmployee.count())
                .from(qEmployee)
                .where(
                        containsNameOrEmail(nameOrEmail),
                        containsEmployeeNumber(employeeNumber),
                        containsPosition(position),
                        betweenHireDate(hireDateFrom, hireDateTo),
                        eqStatus(status)
                )
                .fetchOne();
    }

    // 부분 일치 조건
    // 이름 또는 이메일
    private BooleanExpression containsNameOrEmail(String nameOrEmail) {
        if (StringUtils.isNullOrEmpty(nameOrEmail)) {
            return null;
        }
        return qEmployee.name.contains(nameOrEmail)
                .or(qEmployee.email.contains(nameOrEmail));
    }

    // 사원 번호
    private BooleanExpression containsEmployeeNumber(String employeeNumber) {
        if (StringUtils.isNullOrEmpty(employeeNumber)) {
            return null;
        }
        return qEmployee.employeeNumber.contains(employeeNumber);
    }

    // 부서 이름
    // TODO: department 엔티티 완성 시 부서 이름 조건 추가

    // 직함
    private BooleanExpression containsPosition(String position) {
        if (StringUtils.isNullOrEmpty(position)) {
            return null;
        }
        return qEmployee.position.contains(position);
    }

    // 범위 조건
    // 입사일
    private BooleanExpression betweenHireDate(LocalDate hireDateFrom, LocalDate hireDateTo) {
        if (hireDateFrom != null && hireDateTo != null){
            return qEmployee.hireDate.between(hireDateFrom, hireDateTo);
        } else if (hireDateFrom != null) {
            return qEmployee.hireDate.goe(hireDateFrom);
        } else if (hireDateTo != null) {
            return qEmployee.hireDate.loe(hireDateTo);
        }

        return null;
    }

    // 완전 일치 조건
    // 상태
    private BooleanExpression eqStatus(EmployeeStatus status) {
        if (status == null) {
            return null;
        }
        return qEmployee.status.eq(status);
    }


    // 정렬 기준 설정
    private OrderSpecifier<?> getSortOrder(String sortField, String sortDirection) {
        boolean isDesc = "desc".equalsIgnoreCase(sortDirection);

        switch (sortField) {
            case "name" :
                return isDesc ? qEmployee.name.desc() : qEmployee.name.asc();
            case "employeeNumber" :
                return isDesc ? qEmployee.employeeNumber.desc() : qEmployee.employeeNumber.asc();
            case "hireDate" :
                return isDesc ? qEmployee.hireDate.desc() : qEmployee.hireDate.asc();
        }
        return null;
    }


    // 커서 기반 페이지네이션
    private BooleanExpression cursorCondition(String cursor, String sortField, String sortDirection, Long idAfter) {
        boolean isDesc = "desc".equalsIgnoreCase(sortDirection);

        if (cursor != null) {

            switch (sortField) {
                case "name" :
                    return isDesc ? qEmployee.name.loe(cursor) : qEmployee.name.goe(cursor);
                case "employeeNumber" :
                    return isDesc ? qEmployee.employeeNumber.loe(cursor) : qEmployee.employeeNumber.goe(cursor);
                case "hireDate" :
                    return isDesc ? qEmployee.hireDate.loe(LocalDate.parse(cursor)) : qEmployee.hireDate.goe(LocalDate.parse(cursor));
            }
        } else {
            if (idAfter != null) {
                if (isDesc) {
                    return qEmployee.id.lt(idAfter);
                } else {
                    return qEmployee.id.gt(idAfter);
                }
            }
        }

        return null;
    }
}