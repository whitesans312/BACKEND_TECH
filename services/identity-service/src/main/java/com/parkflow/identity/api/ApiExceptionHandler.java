package com.parkflow.identity.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.parkflow.identity.application.IdentityService.InvalidCredentialsException;
import com.parkflow.identity.shared.ConflictException;
import com.parkflow.identity.shared.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class) ProblemDetail notFound(ResourceNotFoundException e){return problem(HttpStatus.NOT_FOUND,"No encontrado",e.getMessage());}
    @ExceptionHandler(ConflictException.class) ProblemDetail conflict(ConflictException e){return problem(HttpStatus.CONFLICT,"Conflicto",e.getMessage());}
    @ExceptionHandler(InvalidCredentialsException.class) ProblemDetail unauthorized(InvalidCredentialsException e){return problem(HttpStatus.UNAUTHORIZED,"Autenticación fallida","Correo o contraseña inválidos.");}
    @ExceptionHandler(DataIntegrityViolationException.class) ProblemDetail integrity(DataIntegrityViolationException e){return problem(HttpStatus.CONFLICT,"Conflicto de integridad","La operación entra en conflicto con el estado actual de Identity.");}
    @ExceptionHandler(MethodArgumentNotValidException.class) ProblemDetail invalid(MethodArgumentNotValidException e){ProblemDetail p=problem(HttpStatus.BAD_REQUEST,"Solicitud inválida","La solicitud contiene campos inválidos.");p.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).toList());return p;}
    private ProblemDetail problem(HttpStatus status,String title,String detail){ProblemDetail p=ProblemDetail.forStatusAndDetail(status,detail);p.setTitle(title);return p;}
}
