package com.parkflow.billing.api;

import com.parkflow.billing.shared.ConflictException;
import com.parkflow.billing.shared.ResourceNotFoundException;
import com.parkflow.billing.shared.UnprocessableOperationException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class BillingExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException ex) { return problem(HttpStatus.NOT_FOUND, ex.getMessage()); }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) { return problem(HttpStatus.CONFLICT, ex.getMessage()); }

    @ExceptionHandler(UnprocessableOperationException.class)
    ProblemDetail unprocessable(UnprocessableOperationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "La operación entra en conflicto con las reglas de integridad de Billing.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct().collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, details.isBlank() ? "Solicitud inválida." : details);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ProblemDetail invalidRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "La solicitud contiene parámetros ausentes o inválidos.");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setTitle(status.getReasonPhrase());
        result.setType(URI.create("about:blank"));
        return result;
    }
}
