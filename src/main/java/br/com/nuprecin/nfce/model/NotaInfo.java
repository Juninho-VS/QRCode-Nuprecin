package br.com.nuprecin.nfce.model;

import java.math.BigDecimal;

public record NotaInfo(
        String destinoOperacao,
        String consumidorFinal,
        String presencaComprador,
        String modelo,
        String serie,
        String numero,
        String dataEmissao,
        BigDecimal valorTotalServico,
        BigDecimal baseCalculoIcms,
        BigDecimal valorIcms,
        String protocolo
) {
}
