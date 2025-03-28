package team7.hrbank.domain.department.dto;

import java.util.List;

public record PageDepartmentsResponseDto(
  List<DepartmentWithEmployeeCountResponseDto> content,
  String nextCursor,
  Long nextIdAfter,
  Integer size,
  Long totalElements,
  boolean hasNext
) {
  public PageDepartmentsResponseDto(List<DepartmentWithEmployeeCountResponseDto> content, String nextCursor, Long nextIdAfter, Integer size, Long totalElements, boolean hasNext) {
    this.content = content;
    this.nextCursor = nextCursor;
    this.nextIdAfter = nextIdAfter;
    this.size = size;
    this.totalElements = totalElements;
    this.hasNext = hasNext;
  }
}