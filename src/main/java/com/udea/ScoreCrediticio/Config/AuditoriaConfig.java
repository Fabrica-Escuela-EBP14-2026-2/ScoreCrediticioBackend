package com.udea.ScoreCrediticio.Config;

import java.util.List;

import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;
import com.udea.ScoreCrediticio.Auditoria.AuditoriaEventListener;
import com.udea.ScoreCrediticio.Auditoria.AuditoriaIntegrator;

/**
 * Conecta el listener de auditoria con Hibernate sin acoplar las entidades
 * a la infraestructura de bitacora.
 */
@Configuration
public class AuditoriaConfig {

    @Bean
    public HibernatePropertiesCustomizer auditoriaHibernateProperties(ObjectMapper objectMapper) {
        AuditoriaEventListener listener = new AuditoriaEventListener(objectMapper);

        return properties -> properties.put(
                "hibernate.integrator_provider",
                (IntegratorProvider) () -> List.of(new AuditoriaIntegrator(listener)));
    }
}
