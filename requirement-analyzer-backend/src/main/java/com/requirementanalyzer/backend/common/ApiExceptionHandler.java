package com.requirementanalyzer.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.annotation.Order;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Order(-1)
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getStatus().getReasonPhrase());
        problem.setProperty("code", exception.getCode());
        problem.setProperty("instance", request.getRequestURI());
        return problem;
    }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        ProblemDetail handleRequestValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = validationProblem(request);
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
            .toList();
        problem.setProperty("errors", errors);
        return problem;
        }

        @ExceptionHandler(ConstraintViolationException.class)
        ProblemDetail handleConstraintValidation(ConstraintViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", exception.getConstraintViolations().stream()
            .map(error -> Map.of(
                "field", error.getPropertyPath().toString(),
                "message", error.getMessage()))
            .toList());
        return problem;
        }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty("instance", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleMethodValidation(HttpServletRequest request) {
        return validationProblem(request);
    }

    private ProblemDetail validationProblem(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Bad Request");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("instance", request.getRequestURI());
        return problem;
    }
}