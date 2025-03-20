package team7.hrbank.domain.department;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team7.hrbank.domain.department.dto.DepartmentRequestDTO;
import team7.hrbank.domain.department.dto.DepartmentMapper;
import team7.hrbank.domain.department.dto.DepartmentResponseDTO;
import team7.hrbank.domain.department.dto.DepartmentSearchCondition;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    public Department create(DepartmentRequestDTO createDTO) {
        validateDuplicateName(createDTO.name());

        Department department = departmentMapper.toEntity(createDTO);
        return departmentRepository.save(department);
    }

    public Department update(Long id, DepartmentRequestDTO updateDTO) {
        validateDuplicateName(updateDTO.name());
        Department updatingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("부서가 존재하지 않습니다."));

        departmentMapper.update(updateDTO, updatingDepartment);
        return updatingDepartment;
    }
    //- **{이름 또는 설명}**으로 부서 목록을 조회할 수 있습니다.
    //    - **{이름 또는 설명}**는 부분 일치 조건입니다.
    //    - 조회 조건이 여러 개인 경우 모든 조건을 만족한 결과로 조회합니다.
    //- **{이름}**, **{설립일}**로 정렬 및 페이지네이션을 구현합니다.
    //    - 여러 개의 정렬 조건 중 선택적으로 1개의 정렬 조건만 가질 수 있습니다.
    //    - 정확한 페이지네이션을 위해 **{이전 페이지의 마지막 요소 ID}**를 활용합니다.
    //    - 화면을 고려해 적절한 페이지네이션 전략을 선택합니다.

    // 부서 목록 조회하는 메서드
    // search : 조건 기반 검색임이 들어나는 이름이라네요
    public DepartmentResponseDTO searchDepartments(DepartmentSearchCondition condition) {
        return departmentRepository.findPagingAll1(condition);
    }

    // 부서 상세 조회
    public List<Department> findById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("id에 해당하는 부서가 존재하지 않습니다."));

        // employeeCount 필요
        // 다른 방안1. employee 레이어에서 한번에 join해서 가져오기도 가능
        // 다른 방안2. DepartmentRepository에서 Querydsl써서 join해서 가져옥
        return new ArrayList<>();
    }

    public void delete(Long id) {
        //소속된 직원이 없는 경우에만 부서를 삭제할 수 있습니다.
        // 이건 다른 레이어 필요해서 나중에 ㄱ
        departmentRepository.deleteById(id);
    }


    private void validateDuplicateName(String name) {
        if (departmentRepository.existsByName(name)) {
            throw new RuntimeException("이미 존재하는 부서명입니다.");
        }
    }
}
