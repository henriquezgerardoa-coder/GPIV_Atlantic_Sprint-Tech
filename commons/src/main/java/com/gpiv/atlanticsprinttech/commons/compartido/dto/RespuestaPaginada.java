package com.gpiv.atlanticsprinttech.commons.compartido.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record RespuestaPaginada<T>(
    List<T> contenido,
    int pagina,
    int tamanio,
    long totalElementos,
    int totalPaginas,
    boolean esUltima
) {
    public static <T> RespuestaPaginada<T> de(Page<T> pagina) {
        return new RespuestaPaginada<>(
            pagina.getContent(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.isLast()
        );
    }
}