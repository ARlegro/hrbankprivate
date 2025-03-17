package team7.hrbank.common.exception.employee;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team7.hrbank.common.dto.ErrorResponse;
import team7.hrbank.common.exception.ErrorCode;

import java.time.Instant;

@RestControllerAdvice(basePackages = "team7.hrbank.domain.employee")
public class EmployeeException {
    // 400 - Bad Request
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handlerEmailDuplication(DataIntegrityViolationException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                ErrorCode.EMAIL_DUPLICATION.getStatus(),
                ErrorCode.EMAIL_DUPLICATION.getMessage(),
                "이미 존재하는 이메일입니다."
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
