package com.ticket.master.common.exception;

import com.ticket.master.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

public abstract class BaseExceptionHandler {

    @ExceptionHandler(NullableViolation.class)
    public ResponseEntity<ErrorResponse> handleNullableViolation(NullableViolation ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "NULLABLE_VIOLATION", ex.getMessage(), request);
    }

    @ExceptionHandler(UniquenessViolation.class)
    public ResponseEntity<ErrorResponse> handleUniquenessViolation(UniquenessViolation ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "UNIQUENESS_VIOLATION", ex.getMessage(), request);
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponse> handleServerException(ServerException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request);
    }

    protected ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorCode, String message, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                errorCode,
                message,
                path
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}
