package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.exception.ApiException;
import hei.school.cinema.gen.model.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException e, HttpServletRequest request) {
    return toResponse(e.getStatus(), e.getType(), e.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .orElse("Invalid request body");
    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException e, HttpServletRequest request) {
    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleMessageNotReadable(
      HttpMessageNotReadableException e, HttpServletRequest request) {
    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request body", request);
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e,
      HttpServletRequest request) {
    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid parameter value", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolation(
      DataIntegrityViolationException e, HttpServletRequest request) {
    return toResponse(HttpStatus.CONFLICT, "CONFLICT", "Data integrity violation", request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("Unexpected error", e);
    return toResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "Internal server error",
        request);
  }

  private ResponseEntity<ApiError> toResponse(
      HttpStatus status, String type, String message, HttpServletRequest request) {
    ApiError error =
        new ApiError()
            .type(type)
            .message(message)
            .status(status.value())
            .timestamp(OffsetDateTime.now())
            .path(request.getRequestURI());
    return ResponseEntity.status(status).body(error);
  }
}
