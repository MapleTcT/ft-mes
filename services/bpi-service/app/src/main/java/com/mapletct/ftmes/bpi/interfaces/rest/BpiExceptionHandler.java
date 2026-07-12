package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BpiExceptionHandler {

    @ExceptionHandler(BpiNotFoundException.class)
    ProblemDetail notFound(BpiNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), request);
    }

    @ExceptionHandler(BpiForbiddenException.class)
    ProblemDetail forbidden(BpiForbiddenException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(), request);
    }

    @ExceptionHandler(BpiConflictException.class)
    ProblemDetail conflict(BpiConflictException exception, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
        detail.setProperty("currentRevision", exception.getCurrentRevision());
        return detail;
    }

    @ExceptionHandler({BpiValidationException.class, MethodArgumentNotValidException.class})
    ProblemDetail validation(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed", exception.getMessage(), request);
    }

    @ExceptionHandler(BpiPreconditionRequiredException.class)
    ProblemDetail precondition(BpiPreconditionRequiredException exception, HttpServletRequest request) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "Precondition Required", exception.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("traceId", String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE)));
        return problem;
    }
}
