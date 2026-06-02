package br.com.nuprecin.nfce.model;

import java.math.BigDecimal;

public record Totais(
        Integer quantidadeTotalItens,
        BigDecimal valorTotalNota
) {
}
