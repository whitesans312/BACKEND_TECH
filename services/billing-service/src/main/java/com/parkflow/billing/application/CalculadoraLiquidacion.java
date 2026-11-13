package com.parkflow.billing.application;

import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.shared.UnprocessableOperationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraLiquidacion {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");
    private final int minutosGracia;
    private static final int UMBRAL_DIARIO = 180;

    @Autowired
    public CalculadoraLiquidacion(@Value("${billing.parking.grace-minutes:15}") int minutosGracia) {
        if (minutosGracia < 0 || minutosGracia > 1440) {
            throw new IllegalArgumentException("Los minutos de gracia deben estar entre 0 y 1440.");
        }
        this.minutosGracia = minutosGracia;
    }

    CalculadoraLiquidacion() { this(15); }

    public int minutosGracia() { return minutosGracia; }

    public CalculoLiquidacion calcular(Tarifa hora, Tarifa dia, Instant entrada, Instant corte) {
        if (corte.isBefore(entrada)) throw new UnprocessableOperationException("La fecha de corte no puede ser anterior a la entrada.");
        long elapsedSeconds = Duration.between(entrada, corte).getSeconds();
        long elapsed = elapsedSeconds == 0 ? 0 : (elapsedSeconds + 59) / 60;
        if (elapsed > Integer.MAX_VALUE) throw new UnprocessableOperationException("La permanencia excede el rango soportado.");
        int minutes = (int) elapsed;
        int chargeable = Math.max(0, minutes - minutosGracia);
        if (chargeable == 0) return result(minutes, chargeable, 0, BigDecimal.ZERO, "TIEMPO_GRATIS", hora);

        if (chargeable <= UMBRAL_DIARIO) {
            Instant billableStart = entrada.plusSeconds(minutosGracia * 60L);
            int units = Math.toIntExact(hourlyBlocks(billableStart, corte));
            return result(minutes, chargeable, units, hora.getValor().multiply(BigDecimal.valueOf(units)), "COBRO_HORAS", hora);
        }

        Instant billableStart = entrada.plusSeconds(minutosGracia * 60L);
        long cycles = dailyCycles(billableStart, corte);
        return result(minutes, chargeable, Math.toIntExact(cycles), dia.getValor().multiply(BigDecimal.valueOf(cycles)), "TARIFA_DIARIA", dia);
    }

    private long dailyCycles(Instant start, Instant endExclusive) {
        ZonedDateTime localStart = start.atZone(ZONA_NEGOCIO);
        ZonedDateTime localEnd = endExclusive.minusNanos(1).atZone(ZONA_NEGOCIO);
        LocalDateTime startCycle = cycleStart(localStart);
        LocalDateTime endCycle = cycleStart(localEnd);
        return ChronoUnit.DAYS.between(startCycle.toLocalDate(), endCycle.toLocalDate()) + 1;
    }

    private long hourlyBlocks(Instant start, Instant endExclusive) {
        ZonedDateTime localStart=start.atZone(ZONA_NEGOCIO);
        ZonedDateTime localEnd=endExclusive.minusNanos(1).atZone(ZONA_NEGOCIO);
        ZonedDateTime firstBlock=localStart.truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime lastBlock=localEnd.truncatedTo(ChronoUnit.HOURS);
        return ChronoUnit.HOURS.between(firstBlock,lastBlock)+1;
    }

    private LocalDateTime cycleStart(ZonedDateTime value) {
        LocalDateTime local = value.toLocalDateTime();
        LocalDateTime todayAtEight = value.toLocalDate().atTime(LocalTime.of(8, 0));
        return local.isBefore(todayAtEight) ? todayAtEight.minusDays(1) : todayAtEight;
    }

    private CalculoLiquidacion result(int minutes, int chargeable, int units, BigDecimal amount, String concept, Tarifa tariff) {
        BigDecimal total = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (total.precision() > 12) throw new UnprocessableOperationException("El total excede la precisión permitida por la base de datos.");
        return new CalculoLiquidacion(minutes, chargeable, units, total, concept, tariff);
    }
}
