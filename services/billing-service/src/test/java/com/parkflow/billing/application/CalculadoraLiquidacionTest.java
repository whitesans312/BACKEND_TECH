package com.parkflow.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkflow.billing.domain.ConceptoTarifa;
import com.parkflow.billing.domain.Tarifa;
import com.parkflow.billing.domain.TipoVehiculo;
import com.parkflow.billing.shared.UnprocessableOperationException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CalculadoraLiquidacionTest {
    private final CalculadoraLiquidacion calculator=new CalculadoraLiquidacion();
    private final Instant entry=Instant.parse("2026-11-02T13:00:00Z");
    private final Tarifa hour=new Tarifa(TipoVehiculo.CARRO,ConceptoTarifa.HORA,new BigDecimal("6000.00"),"COP",entry);
    private final Tarifa day=new Tarifa(TipoVehiculo.CARRO,ConceptoTarifa.DIA,new BigDecimal("18000.00"),"COP",entry);

    @Test void gracePeriodIsFreeAndIsDeductedBeforeBilling(){
        var result=calculator.calcular(hour,day,entry,entry.plusSeconds(15*60L));
        assertThat(result.minutos()).isEqualTo(15); assertThat(result.minutosFacturables()).isZero();
        assertThat(result.total()).isEqualByComparingTo("0.00"); assertThat(result.concepto()).isEqualTo("TIEMPO_GRATIS");
    }
    @Test void billsAnyFractionOfAnHourAsTheNextFullHour(){
        var result=calculator.calcular(hour,day,entry,entry.plusSeconds(76*60L));
        assertThat(result.minutosFacturables()).isEqualTo(61); assertThat(result.unidades()).isEqualTo(2);
        assertThat(result.total()).isEqualByComparingTo("12000.00");
    }
    @Test void countsClockBlocksTouchedAfterTheGracePeriod(){
        Instant night=Instant.parse("2026-11-02T02:00:00Z"); // 21:00 in Bogota
        var result=calculator.calcular(hour,day,night,night.plusSeconds(70*60L)); // 22:10
        assertThat(result.minutosFacturables()).isEqualTo(55);
        assertThat(result.unidades()).isEqualTo(2);
        assertThat(result.total()).isEqualByComparingTo("12000.00");
    }
    @Test void exactlyThreeFacturableHoursRemainHourly(){
        var result=calculator.calcular(hour,day,entry,entry.plusSeconds(195*60L));
        assertThat(result.minutosFacturables()).isEqualTo(180); assertThat(result.concepto()).isEqualTo("COBRO_HORAS");
        assertThat(result.unidades()).isEqualTo(4); assertThat(result.total()).isEqualByComparingTo("24000.00");
    }
    @Test void moreThanThreeFacturableHoursUsesDailyInsteadOfHours(){
        var result=calculator.calcular(hour,day,entry,entry.plusSeconds(196*60L));
        assertThat(result.minutosFacturables()).isEqualTo(181); assertThat(result.concepto()).isEqualTo("TARIFA_DIARIA");
        assertThat(result.unidades()).isEqualTo(1); assertThat(result.total()).isEqualByComparingTo("18000.00");
    }
    @Test void chargesAFullDailyRateForEachEightAmCycleCrossed(){
        var result=calculator.calcular(hour,day,entry,entry.plusSeconds((24*60+16)*60L));
        assertThat(result.concepto()).isEqualTo("TARIFA_DIARIA"); assertThat(result.unidades()).isEqualTo(2);
        assertThat(result.total()).isEqualByComparingTo("36000.00");
    }
    @Test void rejectsCutoffBeforeEntry(){
        assertThatThrownBy(()->calculator.calcular(hour,day,entry,entry.minusSeconds(1)))
                .isInstanceOf(UnprocessableOperationException.class);
    }
}
