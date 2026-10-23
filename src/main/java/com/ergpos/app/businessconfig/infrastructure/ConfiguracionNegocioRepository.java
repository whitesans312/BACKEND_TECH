package com.ergpos.app.businessconfig.infrastructure;

import com.ergpos.app.businessconfig.domain.ConfiguracionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConfiguracionNegocioRepository extends JpaRepository<ConfiguracionNegocio, String> {
    List<ConfiguracionNegocio> findByCategoria(String categoria);
}
