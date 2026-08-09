package hei.school.cinema.endpoint.rest.controller;

import hei.school.cinema.endpoint.rest.dto.ApiError;
import hei.school.cinema.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(
      ApiException exception, HttpServletRequest request) {

    return toResponse(exception.getStatus(), exception.getType(), exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {

    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Invalid request body");

    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleMessageNotReadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {

    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request body", request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

    return toResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid parameter value", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolation(
      DataIntegrityViolationException exception, HttpServletRequest request) {

    return toResponse(HttpStatus.CONFLICT, "CONFLICT", "Data integrity violation", request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception exception, HttpServletRequest request) {

    log.error("Unexpected error", exception);

    return toResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "Internal server error",
        request);
  }

  private ResponseEntity<ApiError> toResponse(
      HttpStatus status, String type, String message, HttpServletRequest request) {

    ApiError error =
        new ApiError(type, message, status.value(), OffsetDateTime.now(), request.getRequestURI());

    return ResponseEntity.status(status).body(error);
  }
}
