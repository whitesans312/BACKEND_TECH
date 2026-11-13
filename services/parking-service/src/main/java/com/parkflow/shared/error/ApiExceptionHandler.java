package com.parkflow.shared.error;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleMissingRoute(NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", "No existe la ruta solicitada.");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Conflicto", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "La solicitud contiene campos inválidos.");
        detail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo JSON está mal formado o contiene valores no admitidos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                "Uno de los parámetros o identificadores tiene un formato inválido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Parámetro requerido",
                "Falta un parámetro requerido en la solicitud.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        HttpHeaders headers = new HttpHeaders();
        if (exception.getSupportedHttpMethods() != null) {
            headers.setAllow(exception.getSupportedHttpMethods());
        }
        return new ResponseEntity<>(problem(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método HTTP no está disponible para esta ruta."), headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato no admitido",
                "El tipo de contenido de la solicitud no está admitido.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ProblemDetail handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return problem(HttpStatus.NOT_ACCEPTABLE, "Formato de respuesta no admitido",
                "No hay una representación disponible para el encabezado Accept enviado.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        String sqlState = findSqlState(exception);
        if (sqlState != null && sqlState.startsWith("23")) {
            return problem(HttpStatus.CONFLICT, "Conflicto de integridad",
                    "La operación entra en conflicto con el estado actual de Parking.");
        }
        if (sqlState != null && sqlState.startsWith("22")) {
            return problem(HttpStatus.BAD_REQUEST, "Dato inválido",
                    "Uno de los valores no es válido para el esquema de Parking.");
        }

        logger.error("Error de persistencia no clasificado en Parking.", exception);
        return internalError();
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        logger.error("Error inesperado al procesar una solicitud de Parking.", exception);
        return internalError();
    }

    private ProblemDetail internalError() {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error interno al procesar la solicitud.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private String findSqlState(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }
}
