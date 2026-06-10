package com.gpiv.atlanticsprinttech.backend.configuracion;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * Configuración de Spring Cache para caché a nivel de aplicación.
 * Complementa el L2 Cache de Hibernate.
 *
 * @since 2026-06-04
 */
@Configuration
@EnableCaching
public class ConfiguracionCache {

    /**
     * Configura el CacheManager para Spring.
     * Utiliza en-memory cache para datos que cambian frecuentemente a nivel aplicación.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "empresas",
            "usuarios",
            "lotes",
            "radicaciones",
            "roles",
            "catalogos"
        );
    }
}

