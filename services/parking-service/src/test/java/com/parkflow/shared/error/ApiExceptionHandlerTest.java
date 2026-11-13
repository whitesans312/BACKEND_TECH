package com.parkflow.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private Logger handlerLogger;
    private Level previousLevel;

    @BeforeEach
    void suppressExpectedErrorLog() {
        handlerLogger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        previousLevel = handlerLogger.getLevel();
        handlerLogger.setLevel(Level.OFF);
    }

    @AfterEach
    void restoreHandlerLogLevel() {
        handlerLogger.setLevel(previousLevel);
    }

    @Test
    void mapsDatabaseConstraintConflictsTo409WithoutExposingSqlDetails() {
        var detail = handler.handleDataIntegrity(
                new DataIntegrityViolationException("private constraint name", new SQLException("private sql", "23505")));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getDetail()).doesNotContain("private");
    }

    @Test
    void doesNotMapUnclassifiedDatabaseFailuresTo409() {
        var detail = handler.handleDataIntegrity(
                new DataIntegrityViolationException("private database detail", new SQLException("private sql", "42000")));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getDetail()).doesNotContain("private");
    }
}
