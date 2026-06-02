package br.com.nuprecin.nfce;

import br.com.nuprecin.nfce.model.Nfce;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NfceParserTest {

    @Test
    void parse_deveExtrairConsumidorChaveEmitenteItensETotais() {
        String html = """
                <html>
                  <body>
                    <table class=\"table text-center\">
                      <tbody>
                        <tr><td style=\"border-top: 0px;\">CNPJ: 04641376013467 -, Inscrição Estadual: 0020488293197</td></tr>
                        <tr><td style=\"border-top: 0px; display: block; font-style: italic;\">AV. FRANCISCO NEGRAO DE LIMA, 533, CEU AZUL, 3106200 - BELO HORIZONTE, MG</td></tr>
                      </tbody>
                    </table>

                    <div class=\"panel panel-default\">
                      <div class=\"panel-heading\"><h4 class=\"panel-title\">Consumidor</h4></div>
                      <div class=\"collapse\" role=\"tabpanel\">
                        <table class=\"table\">
                          <thead><tr><th>Nome / Razão Social</th><th>CPF</th><th>UF</th></tr></thead>
                          <tbody><tr><td></td><td>70193660660</td><td></td></tr></tbody>
                        </table>
                      </div>
                    </div>

                    <div class=\"panel panel-default\">
                      <div class=\"panel-heading\"><h4 class=\"panel-title\">Chave de acesso</h4></div>
                      <div class=\"collapse\" role=\"tabpanel\">
                        <table class=\"table\"><tbody><tr><td>31-26/04-04.641.376/0134-67-65-070-000.231.427-135.112.4718</td></tr></tbody></table>
                      </div>
                    </div>

                    <div class=\"panel panel-default\">
                      <div class=\"panel-heading\"><h4 class=\"panel-title\">Informações Complementares de Interesse do Contribuinte</h4></div>
                      <div class=\"collapse\" role=\"tabpanel\">
                        <table class=\"table\"><thead><tr><th>Descrição</th></tr></thead><tbody><tr><td>Texto complementar</td></tr></tbody></table>
                      </div>
                    </div>

                    <div class=\"panel panel-default\">
                      <div class=\"panel-heading\"><h4 class=\"panel-title\">Informações gerais da Nota</h4></div>
                      <div class=\"collapse\" role=\"tabpanel\">
                        <table class=\"table\">
                          <thead><tr><th>Nome / Razão Social</th><th>CNPJ</th><th>Inscrição Estadual</th><th>UF</th></tr></thead>
                          <tbody><tr><td>EMPRESA X</td><td>04641376013467</td><td>0020488293197</td><td>MG</td></tr></tbody>
                        </table>

                        <table class=\"table\">
                          <thead><tr><th>Destino da operação</th><th>Consumidor final</th><th>Presença do Comprador</th></tr></thead>
                          <tbody><tr><td>1 - Operação Interna</td><td>1 - Sim</td><td>1 - Operação presencial</td></tr></tbody>
                        </table>

                        <table class=\"table\">
                          <thead><tr><th>Modelo</th><th>Série</th><th>Número</th><th>Data Emissão</th></tr></thead>
                          <tbody><tr><td>65</td><td>70</td><td>231427</td><td>19/04/2026 10:57:21</td></tr></tbody>
                        </table>

                        <table class=\"table\">
                          <thead><tr><th>Valor total do serviço</th><th>Base de Cálculo ICMS</th><th>Valor ICMS</th></tr></thead>
                          <tbody><tr><td>R$ 42,21</td><td>R$ 0,00</td><td>R$ 0,00</td></tr></tbody>
                        </table>

                        <table class=\"table\">
                          <thead><tr><th>Protocolo</th></tr></thead>
                          <tbody><tr><td>131260697043739</td></tr></tbody>
                        </table>
                      </div>
                    </div>

                    <table class=\"table\"><tbody id=\"myTable\">
                      <tr>
                        <td><h7>ITEM A</h7> (Código: 123)</td>
                        <td>Qtde total de ítens: 1.000</td>
                        <td>UN: UN</td>
                        <td>Valor total R$: R$ 6,48</td>
                      </tr>
                      <tr>
                        <td><h7>ITEM B</h7> (Código: 456)</td>
                        <td>Qtde total de ítens: 2.000</td>
                        <td>UN: UN</td>
                        <td>Valor total R$: R$ 35,73</td>
                      </tr>
                    </tbody></table>

                    <div class=\"row\"><div><strong>Qtde total de ítens</strong></div><div><strong>2</strong></div></div>
                    <div class=\"row\"><div><strong>Valor total R$</strong></div><div><strong>42.21</strong></div></div>
                  </body>
                </html>
                """;

        Nfce nfce = NfceParser.parse(html, "https://exemplo/qrcode.xhtml?p=31260404641376013467650700002314271351124718|2|1");

        assertEquals("31260404641376013467650700002314271351124718", nfce.chaveAcesso());
        assertNotNull(nfce.consumidor());
        assertEquals("70193660660", nfce.consumidor().cpf());

        assertNotNull(nfce.estabelecimento());
        assertEquals("EMPRESA X", nfce.estabelecimento().nome());
        assertEquals("04641376013467", nfce.estabelecimento().cnpj());
        assertEquals("AV. FRANCISCO NEGRAO DE LIMA, 533, CEU AZUL, 3106200 - BELO HORIZONTE, MG", nfce.estabelecimento().enderecoCompleto());
        assertEquals("AV. FRANCISCO NEGRAO DE LIMA", nfce.estabelecimento().logradouro());
        assertEquals("533", nfce.estabelecimento().numero());
        assertEquals("CEU AZUL", nfce.estabelecimento().bairro());
        assertEquals("BELO HORIZONTE", nfce.estabelecimento().municipio());
        assertEquals("MG", nfce.estabelecimento().uf());
        assertEquals("3106200", nfce.estabelecimento().cep());

        assertNotNull(nfce.nota());
        assertEquals("65", nfce.nota().modelo());
        assertEquals("70", nfce.nota().serie());
        assertEquals("231427", nfce.nota().numero());
        assertEquals("19/04/2026 10:57:21", nfce.nota().dataEmissao());
        assertEquals(new BigDecimal("42.21"), nfce.nota().valorTotalServico());
        assertEquals("131260697043739", nfce.nota().protocolo());

        assertNotNull(nfce.itens());
        assertEquals(2, nfce.itens().size());
        assertEquals("ITEM A", nfce.itens().get(0).descricao());
        assertEquals("123", nfce.itens().get(0).codigo());
        assertEquals(new BigDecimal("1.000"), nfce.itens().get(0).quantidade());
        assertEquals(new BigDecimal("6.48"), nfce.itens().get(0).valorTotal());

        assertNotNull(nfce.totais());
        assertEquals(2, nfce.totais().quantidadeTotalItens());
        assertEquals(new BigDecimal("42.21"), nfce.totais().valorTotalNota());

        assertEquals("Texto complementar", nfce.informacoesComplementares());
    }
}
