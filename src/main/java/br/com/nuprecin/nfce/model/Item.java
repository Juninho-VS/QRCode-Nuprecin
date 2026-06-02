package br.com.nuprecin.nfce.model;

import java.math.BigDecimal;

public record Item(
        int numero,
        String descricao,
        String codigo,
        BigDecimal quantidade,
        String unidade,
        BigDecimal valorTotal
) {
}
