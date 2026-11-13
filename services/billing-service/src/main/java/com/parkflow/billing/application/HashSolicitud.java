package com.parkflow.billing.application;

import com.parkflow.billing.api.CrearLiquidacionRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class HashSolicitud {
    public String calcular(CrearLiquidacionRequest request) {
        String canonical = String.join("|",
                request.sesionId().toString().toLowerCase(),
                request.numeroTicket(),
                request.placa(),
                request.tipoVehiculo().name(),
                DateTimeFormatter.ISO_INSTANT.format(request.fechaEntrada()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", impossible);
        }
    }
}
