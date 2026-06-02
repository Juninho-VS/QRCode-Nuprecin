package br.com.nuprecin.nfce.model;

import java.util.List;

public record Nfce(
        String urlQrcode,
        String chaveAcesso,
        Estabelecimento estabelecimento,
        Consumidor consumidor,
        NotaInfo nota,
        Totais totais,
        List<Item> itens,
        String informacoesComplementares
) {
}
