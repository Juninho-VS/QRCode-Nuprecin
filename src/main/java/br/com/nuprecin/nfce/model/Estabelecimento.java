package br.com.nuprecin.nfce.model;

public record Estabelecimento(
        String nome,
        String cnpj,
        String inscricaoEstadual,
        String uf,
        String enderecoCompleto,
        String logradouro,
        String numero,
        String bairro,
        String municipio,
        String cep
) {
}
