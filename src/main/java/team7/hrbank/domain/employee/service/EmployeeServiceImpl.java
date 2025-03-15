package team7.hrbank.domain.employee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import team7.hrbank.common.dto.PageResponse;
import team7.hrbank.domain.binary.BinaryContent;
import team7.hrbank.domain.binary.BinaryContentService;
import team7.hrbank.domain.employee.dto.EmployeeCreateRequest;
import team7.hrbank.domain.employee.dto.EmployeeDto;
import team7.hrbank.domain.employee.dto.EmployeeUpdateRequest;
import team7.hrbank.domain.employee.entity.Employee;
import team7.hrbank.domain.employee.entity.EmployeeStatus;
import team7.hrbank.domain.employee.repository.CustomEmployeeRepository;
import team7.hrbank.domain.employee.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    // 의존성 주입
    private final EmployeeRepository employeeRepository;
    private final CustomEmployeeRepository customEmployeeRepository;
    private final BinaryContentService binaryContentService;

    // 직원 등록
    @Override
    public EmployeeDto create(EmployeeCreateRequest request, MultipartFile profile) {

        // TODO: ChangeLog에 memo 저장

        // 사원번호 생성
        int year = request.hireDate().getYear();   // 입사 연도
        long count = employeeRepository.countByHireDateYear(year);  // 해당 연도 입사자 수
        String employeeNumber = String.format("EMP-%d-%03d", year, count + 1);  // 최종 사원번호

        // 프로필 사진
        BinaryContent binaryContent = null;
//        if (profile != null) {
//            binaryContent = profileProcess(profile);
//        }

        // Employee 생성
        Employee employee = new Employee(binaryContent, employeeNumber, request.name(), request.email(), request.position(), request.hireDate());

        // DB 저장
        employeeRepository.save(employee);

        // employeeDto로 반환
        return EmployeeDto.fromEntity(employee);
    }

    // 직원 목록 조회
    @Override
    public PageResponse<EmployeeDto> find(String nameOrEmail,
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

        // 다음 페이지 있는지 확인하기 위해 size+1개의 데이터 읽어옴
        List<Employee> employees = customEmployeeRepository.findEmployees(nameOrEmail, employeeNumber, departmentName, position, hireDateFrom, hireDateTo, status,
                idAfter, cursor, size + 1, sortField, sortDirection);

        // 다음 페이지 정보
        String nextCursor = null;
        Long nextIdAfter = null;
        boolean hasNext = false;

        // 전체 데이터 개수 계산
        long totalElement = customEmployeeRepository.totalCountEmployee(nameOrEmail, employeeNumber, departmentName, position, hireDateFrom, hireDateTo, status);

        // 다음 데이터 있는지 확인
        if (employees.size() > size) {  // 읽어온 데이터의 크기가 size보다 큰 경우 -> 다음 페이지 있음
            Employee nextEmployee = employees.get(employees.size() - 1);
            nextIdAfter = nextEmployee.getId(); // 다음 페이지 첫번째 직원의 id
            nextCursor = getSortValue(nextEmployee, sortField); // 다음 페이지 첫번째 직원의 cursor 정보(name, employeeNumber, hireDate)
            hasNext = true; // 다음 페이지 유무

            employees.remove(employees.size() - 1); // size를 초과하는 데이터(마지막 데이터)는 필요없으므로 list에서 삭제
        }

        // TODO: EmployeeMapper 만들고 수정 필요
        List<EmployeeDto> employeeDtos = employees.stream()
                .map(EmployeeDto::fromEntity)
                .toList();


        return new PageResponse<>(
                employeeDtos,
                nextCursor,
                nextIdAfter,
                size,
                (int) totalElement,
                hasNext
        );
    }

    // 직원 상세 조회
    @Override
    public EmployeeDto findById(Long id) {

        Employee employee = employeeRepository.findById(id).orElseThrow();  // TODO: null일 경우 예외 처리

        return EmployeeDto.fromEntity(employee);
    }

    // 직원 수정
    @Override
    public EmployeeDto updateById(Long id, EmployeeUpdateRequest request, MultipartFile profile) {

        // TODO: ChangeLog에 수정 이력 저장

        Employee employee = employeeRepository.findById(id).orElseThrow();  // TODO: null일 경우 예외처리

        // TODO: departmentId 수정 로직 추가

        if (request.name() != null) {
            employee.updateName(request.name());
        }
        if (request.email() != null) {
            employee.updateEmail(request.email());
        }
        if (request.position() != null) {
            employee.updatePosition(request.position());
        }
        if (request.hireDate() != null) {
            employee.updateHireDate(request.hireDate());
        }
        if (request.status() != null) {
            employee.updateStatus(request.status());
        }

//        if (profile != null) {
//            BinaryContent binaryContent = profileProcess(profile);
//            employee.updateProfile(binaryContent);
//        }

        // DB 저장
        employeeRepository.save(employee);

        // employeeDto로 반환
        return EmployeeDto.fromEntity(employee);
    }

    // 직원 삭제
    @Override
    public void deleteById(Long id) {
        employeeRepository.deleteById(id);
    }

    // save() 시 파일 저장 오류 발생으로 주석 처리
//    // 프로필 사진 가공
//    private BinaryContent profileProcess(MultipartFile profile) {
//        try {
//            String fileName = profile.getOriginalFilename();
//            String fileType = profile.getContentType();
//            Long fileSize = profile.getSize();
//            byte[] bytes = profile.getBytes();
//
//            BinaryContentDto binaryContentDto = new BinaryContentDto(fileName, fileType, fileSize, bytes);
//
//            return binaryContentService.save(binaryContentDto);
//
//        } catch (IOException e) {
//            throw new IllegalArgumentException("파일 변환 실패");
//        }
//    }

    private String getSortValue(Employee employee, String sortField) {
        switch (sortField) {
            case "name":
                return employee.getName();
            case "employeeNumber":
                return employee.getEmployeeNumber();
            case "hireDate":
                return employee.getHireDate().toString();
            default:
                return null;
        }
    }
}
