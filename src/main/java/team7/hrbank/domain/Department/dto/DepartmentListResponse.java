package team7.hrbank.domain.Department.dto;

import team7.hrbank.domain.Department.Department;

import java.util.List;

public record DepartmentListResponse(
    List<DepartmentResponse> content,
    String nextCursor,
    Integer nextIdAfter,
    Integer size,
    Long totalElements,
    boolean hasNext
) {
    public DepartmentListResponse(List<DepartmentResponse> content, String nextCursor, Integer nextIdAfter, Integer size, Long totalElements, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.nextIdAfter = nextIdAfter;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }
}