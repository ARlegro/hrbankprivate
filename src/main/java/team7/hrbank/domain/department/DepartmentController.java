package team7.hrbank.domain.department;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import team7.hrbank.domain.department.dto.DepartmentRequestDTO;
import team7.hrbank.domain.department.dto.DepartmentResponseDTO;
import team7.hrbank.domain.department.dto.DepartmentSearchCondition;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public void create(@Valid @RequestBody DepartmentRequestDTO createDTO) {
        log.info("createDTO = {}", createDTO);
        departmentService.create(createDTO);
    }

    @PostMapping("/test-batch")
    public void createBatch(@Valid @RequestBody List<DepartmentRequestDTO> createDTO) {
        log.info("createDTO = {}", createDTO);
        for (DepartmentRequestDTO departmentRequestDTO : createDTO) {
            departmentService.create(departmentRequestDTO);
        }
    }

    @GetMapping
    public DepartmentResponseDTO getDepartment(@Valid DepartmentSearchCondition condition) {
        // 부서 목록조회
        log.info("condition = {}", condition);
        return departmentService.searchDepartments(condition);
    }

}
