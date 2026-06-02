package br.com.nuprecin.nfce;

import br.com.nuprecin.nfce.model.Consumidor;
import br.com.nuprecin.nfce.model.Estabelecimento;
import br.com.nuprecin.nfce.model.Item;
import br.com.nuprecin.nfce.model.Nfce;
import br.com.nuprecin.nfce.model.NotaInfo;
import br.com.nuprecin.nfce.model.Totais;
import br.com.nuprecin.nfce.util.ParsingUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NfceParser {
    private static final Pattern CHAVE_44_DIGITOS = Pattern.compile("\\b\\d{44}\\b");
    private static final Pattern CEP_LABEL = Pattern.compile("(?i)\\bcep\\s*[:\\-]?\\s*(\\d{5}-?\\d{3})\\b");
    private static final Pattern CEP_PLAIN_HYPHEN = Pattern.compile("\\b(\\d{5}-\\d{3})\\b");
    private static final Pattern CEP_PLAIN_8 = Pattern.compile("\\b(\\d{8})\\b");
    private static final Pattern CEP_PLAIN_7 = Pattern.compile("\\b(\\d{7})\\b");
    private static final Pattern MUNICIPIO_UF_AFTER_HYPHEN = Pattern.compile("(?i)^(.*)\\s-\\s*([^,/]+?)\\s*(?:,|/)\\s*([A-Z]{2})\\s*$");

    /**
     * Classe utilitaria; nao sao necessarias instancias.
     */
    private NfceParser() {
    }

    /**
     * Faz o parse do HTML da NFC-e e monta o modelo usado na saida da CLI.
     */
    public static Nfce parse(String html, String qrcodeUrl) {
        Document doc = Jsoup.parse(html);

        Map<String, Element> panels = extractPanels(doc);

        Element consumidorPanel = panels.get(ParsingUtils.normalizeKey("Consumidor"));
        Element chavePanel = panels.get(ParsingUtils.normalizeKey("Chave de acesso"));
        Element complementaresPanel = panels.get(ParsingUtils.normalizeKey("Informações Complementares de Interesse do Contribuinte"));
        Element informacoesNotaPanel = panels.get(ParsingUtils.normalizeKey("Informações gerais da Nota"));

        Consumidor consumidor = parseConsumidor(consumidorPanel, doc);
        String chaveAcesso = parseChaveAcesso(chavePanel, qrcodeUrl, doc);
        String infoComplementares = parseInformacoesComplementares(complementaresPanel);
        Estabelecimento estabelecimento = parseEmitente(informacoesNotaPanel, doc);
        NotaInfo nota = parseNotaInfo(informacoesNotaPanel);
        List<Item> itens = parseItens(doc);
        Totais totais = parseTotais(doc, nota, itens);

        return new Nfce(
                ParsingUtils.blankToNull(qrcodeUrl),
                chaveAcesso,
                estabelecimento,
                consumidor,
                nota,
                totais,
                itens,
                infoComplementares
        );
    }

    /**
     * Agrupa os painéis colapsáveis pelo titulo para que cada secao seja parseada separadamente.
     */
    private static Map<String, Element> extractPanels(Document doc) {
        Map<String, Element> byTitle = new HashMap<>();
        for (Element panel : doc.select("div.panel.panel-default")) {
            Element titleEl = panel.selectFirst(".panel-title");
            Element bodyEl = panel.selectFirst("div.collapse[role=tabpanel]");
            if (titleEl == null || bodyEl == null) {
                continue;
            }
            byTitle.put(ParsingUtils.normalizeKey(titleEl.text()), bodyEl);
        }
        return byTitle;
    }

    /**
     * Faz o parse do bloco do consumidor, tentando primeiro o painel dedicado e depois o documento inteiro.
     */
    private static Consumidor parseConsumidor(Element panelBody, Document doc) {
        Consumidor fromPanel = parseConsumidorFromScope(panelBody);
        if (hasAnyValue(fromPanel)) {
            return fromPanel;
        }

        // Fallback: em alguns HTMLs, o painel pode não existir/ser diferente.
        Consumidor fromDoc = parseConsumidorFromScope(doc);
        return hasAnyValue(fromDoc) ? fromDoc : fromPanel;
    }

    /**
     * Faz o parse dos dados do consumidor a partir de um escopo limitado do HTML.
     */
    private static Consumidor parseConsumidorFromScope(Element scope) {
        if (scope == null) {
            return new Consumidor(null, null, null);
        }

        Element table = findTableByRequiredHeaders(scope, "cpf");
        if (table == null) {
            table = scope.selectFirst("table");
        }
        if (table == null) {
            return new Consumidor(null, null, null);
        }

        Map<String, String> row = extractSingleRowTable(table);

        String nome = pickValue(row, "nome / razão social", "nome / razao social", "nome", "razao social");
        String cpf = pickValue(row, "cpf");
        String uf = pickValue(row, "uf");

        return new Consumidor(
                ParsingUtils.blankToNull(nome),
                ParsingUtils.blankToNull(ParsingUtils.digitsOnly(cpf)),
                ParsingUtils.blankToNull(uf)
        );
    }

    /**
     * Verifica se ao menos um campo do consumidor foi encontrado.
     */
    private static boolean hasAnyValue(Consumidor consumidor) {
        if (consumidor == null) {
            return false;
        }
        return consumidor.nomeRazaoSocial() != null || consumidor.cpf() != null || consumidor.uf() != null;
    }

    /**
     * Extrai a chave de acesso de 44 digitos do painel, da URL ou do texto completo da pagina.
     */
    private static String parseChaveAcesso(Element panelBody, String qrcodeUrl, Document doc) {
        String fromPanel = null;
        if (panelBody != null) {
            Element td = panelBody.selectFirst("table td");
            if (td != null) {
                String digits = ParsingUtils.digitsOnly(td.text());
                if (digits != null && digits.length() == 44) {
                    fromPanel = digits;
                }
            }
        }
        if (fromPanel != null) {
            return fromPanel;
        }

        String fromUrl = ParsingUtils.extractChaveAcessoFromUrl(qrcodeUrl);
        if (fromUrl != null) {
            return fromUrl;
        }

        Matcher m = CHAVE_44_DIGITOS.matcher(doc.text());
        return m.find() ? m.group() : null;
    }

    /**
     * Lê o bloco de informacoes complementares quando ele existir.
     */
    private static String parseInformacoesComplementares(Element panelBody) {
        if (panelBody == null) {
            return null;
        }
        Element td = panelBody.selectFirst("table tbody td");
        return td == null ? null : ParsingUtils.blankToNull(td.text());
    }

    /**
     * Faz o parse dos dados do emitente e separa o endereco completo em campos estruturados.
     */
    private static Estabelecimento parseEmitente(Element informacoesNotaPanel, Document doc) {
        String enderecoCompleto = parseEnderecoEmitente(doc, informacoesNotaPanel);
        EnderecoParts enderecoParts = parseEnderecoCompleto(enderecoCompleto);

        if (informacoesNotaPanel == null) {
            return new Estabelecimento(
                    null,
                    null,
                    null,
                    enderecoParts.uf(),
                    enderecoCompleto,
                    enderecoParts.logradouro(),
                    enderecoParts.numero(),
                    enderecoParts.bairro(),
                    enderecoParts.municipio(),
                    enderecoParts.cep()
            );
        }

        Element emitenteTable = findTableByRequiredHeaders(informacoesNotaPanel,
                "nome / razão social",
                "cnpj",
                "inscrição estadual",
                "uf");

        if (emitenteTable == null) {
            return new Estabelecimento(
                    null,
                    null,
                    null,
                    enderecoParts.uf(),
                    enderecoCompleto,
                    enderecoParts.logradouro(),
                    enderecoParts.numero(),
                    enderecoParts.bairro(),
                    enderecoParts.municipio(),
                    enderecoParts.cep()
            );
        }

        Map<String, String> row = extractSingleRowTable(emitenteTable);
        String nome = pickValue(row, "nome / razão social", "nome / razao social");
        String cnpj = pickValue(row, "cnpj");
        String ie = pickValue(row, "inscrição estadual", "inscricao estadual");
        String uf = ParsingUtils.blankToNull(pickValue(row, "uf"));
        if (uf == null) {
            uf = enderecoParts.uf();
        }

        return new Estabelecimento(
                ParsingUtils.blankToNull(nome),
                ParsingUtils.blankToNull(ParsingUtils.digitsOnly(cnpj)),
                ParsingUtils.blankToNull(ParsingUtils.digitsOnly(ie)),
                ParsingUtils.blankToNull(uf),
                enderecoCompleto,
                enderecoParts.logradouro(),
                enderecoParts.numero(),
                enderecoParts.bairro(),
                enderecoParts.municipio(),
                enderecoParts.cep()
        );
    }

    /**
     * Partes intermediarias do endereco extraidas do endereco completo do emitente.
     */
    private record EnderecoParts(
            String logradouro,
            String numero,
            String bairro,
            String municipio,
            String uf,
            String cep
    ) {
    }

    /**
     * Separa uma string de endereco bruto em logradouro, numero, bairro, cidade, UF e CEP.
     */
    private static EnderecoParts parseEnderecoCompleto(String enderecoCompleto) {
        if (enderecoCompleto == null || enderecoCompleto.isBlank()) {
            return new EnderecoParts(null, null, null, null, null, null);
        }

        String working = enderecoCompleto.trim().replaceAll("\\s+", " ");

        String cep = null;
        Matcher cepMatcher = CEP_LABEL.matcher(working);
        if (cepMatcher.find()) {
            cep = ParsingUtils.digitsOnly(cepMatcher.group(1));
            working = cepMatcher.replaceFirst("").trim();
        } else {
            for (Pattern p : List.of(CEP_PLAIN_HYPHEN, CEP_PLAIN_8, CEP_PLAIN_7)) {
                Matcher m = p.matcher(working);
                if (!m.find()) {
                    continue;
                }
                cep = ParsingUtils.digitsOnly(m.group(1));
                working = m.replaceFirst("").trim();
                break;
            }
        }

        working = cleanupAddressString(working);

        String left = working;
        String municipio = null;
        String uf = null;

        Matcher cityMatcher = MUNICIPIO_UF_AFTER_HYPHEN.matcher(working);
        if (cityMatcher.matches()) {
            left = cityMatcher.group(1);
            municipio = cityMatcher.group(2);
            uf = cityMatcher.group(3).toUpperCase(Locale.ROOT);
        } else {
            String[] commaParts = Arrays.stream(working.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
            if (commaParts.length >= 2) {
                String ufCandidate = commaParts[commaParts.length - 1];
                if (ufCandidate.length() == 2) {
                    uf = ufCandidate.toUpperCase(Locale.ROOT);
                    municipio = commaParts[commaParts.length - 2];
                    left = String.join(", ", Arrays.copyOfRange(commaParts, 0, commaParts.length - 2));
                }
            }
        }

        left = cleanupAddressString(left);

        String logradouro = null;
        String numero = null;
        String bairro = null;

        String[] segments = Arrays.stream(left.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);

        if (segments.length >= 1) {
            logradouro = segments[0];
        }
        if (segments.length >= 2) {
            numero = segments[1];
        }
        if (segments.length >= 3) {
            bairro = String.join(", ", Arrays.copyOfRange(segments, 2, segments.length));
        }

        return new EnderecoParts(
                ParsingUtils.blankToNull(logradouro),
                ParsingUtils.blankToNull(numero),
                ParsingUtils.blankToNull(bairro),
                ParsingUtils.blankToNull(municipio),
                ParsingUtils.blankToNull(uf),
                ParsingUtils.blankToNull(cep)
        );
    }

    /**
     * Normaliza espacos e pontuacao para tornar o parse do endereco mais confiavel.
     */
    private static String cleanupAddressString(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return s;
        }

        s = s.replaceAll("\\s+", " ");
        s = s.replaceAll("\\s*,\\s*", ", ");
        s = s.replaceAll(",\\s*-\\s*", " - ");
        s = s.replaceAll("\\s-\\s", " - ");
        s = s.replaceAll("\\s+", " ").trim();

        s = s.replaceAll("^[,\\-\\s]+", "");
        s = s.replaceAll("[,\\-\\s]+$", "");
        return s;
    }

    /**
     * Localiza o endereco do emitente usando os locais mais comuns do HTML.
     */
    private static String parseEnderecoEmitente(Document doc, Element informacoesNotaPanel) {
        String fromHeader = parseEnderecoEmitenteFromHeaderTable(doc);
        if (fromHeader != null) {
            return fromHeader;
        }

        // Fallback: caso algum HTML traga endereço dentro do painel de informações.
        String fromPanel = parseEnderecoEmitenteFromTables(informacoesNotaPanel);
        if (fromPanel != null) {
            return fromPanel;
        }

        return null;
    }

    /**
     * Extrai o endereco do emitente da tabela de cabecalho/resumo quando ela existir.
     */
    private static String parseEnderecoEmitenteFromHeaderTable(Document doc) {
        if (doc == null) {
            return null;
        }

        Element headerTable = doc.selectFirst("table.table.text-center");
        if (headerTable == null) {
            headerTable = doc.selectFirst("table.text-center");
        }
        if (headerTable == null) {
            for (Element table : doc.select("table")) {
                if (table.text().contains("NFC-e")) {
                    headerTable = table;
                    break;
                }
            }
        }
        if (headerTable == null) {
            return null;
        }

        Element italicTd = headerTable.selectFirst("tbody td[style*=italic]");
        if (italicTd != null) {
            return ParsingUtils.blankToNull(italicTd.text());
        }

        for (Element tr : headerTable.select("tbody tr")) {
            String text = ParsingUtils.blankToNull(tr.text());
            if (text == null) {
                continue;
            }
            if (text.contains("CNPJ") || ParsingUtils.normalizeKey(text).contains("cnpj:")) {
                continue;
            }
            return text;
        }

        return null;
    }

    /**
     * Extrai o endereco do emitente de qualquer tabela que pareca um bloco de endereco.
     */
    private static String parseEnderecoEmitenteFromTables(Element scope) {
        if (scope == null) {
            return null;
        }

        for (Element table : scope.select("table")) {
            List<String> headers = table.select("thead th").eachText();
            if (headers.isEmpty()) {
                continue;
            }

            boolean looksLikeAddress = false;
            for (String h : headers) {
                String key = ParsingUtils.normalizeKey(h);
                if (key.contains("endereco") || key.contains("logradouro") || key.contains("bairro") || key.contains("cep")) {
                    looksLikeAddress = true;
                    break;
                }
            }
            if (!looksLikeAddress) {
                continue;
            }

            Element td = table.selectFirst("tbody td");
            if (td != null) {
                String value = ParsingUtils.blankToNull(td.text());
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * Faz o parse do bloco geral da nota, incluindo operacao e metadados fiscais.
     */
    private static NotaInfo parseNotaInfo(Element informacoesNotaPanel) {
        if (informacoesNotaPanel == null) {
            return new NotaInfo(null, null, null, null, null, null, null, null, null, null, null);
        }

        Element operacaoTable = findTableByRequiredHeaders(informacoesNotaPanel,
                "destino da operação",
                "consumidor final",
                "presença do comprador");
        Map<String, String> operacao = operacaoTable == null ? Map.of() : extractSingleRowTable(operacaoTable);

        Element identificacaoTable = findTableByRequiredHeaders(informacoesNotaPanel,
                "modelo",
                "série",
                "número",
                "data emissão");
        Map<String, String> identificacao = identificacaoTable == null ? Map.of() : extractSingleRowTable(identificacaoTable);

        Element valoresTable = findTableByRequiredHeaders(informacoesNotaPanel,
                "valor total do serviço",
                "base de cálculo icms",
                "valor icms");
        Map<String, String> valores = valoresTable == null ? Map.of() : extractSingleRowTable(valoresTable);

        Element protocoloTable = findTableByRequiredHeaders(informacoesNotaPanel, "protocolo");
        Map<String, String> protocolo = protocoloTable == null ? Map.of() : extractSingleRowTable(protocoloTable);

        return new NotaInfo(
                ParsingUtils.blankToNull(pickValue(operacao, "destino da operação", "destino da operacao")),
                ParsingUtils.blankToNull(pickValue(operacao, "consumidor final")),
                ParsingUtils.blankToNull(pickValue(operacao, "presença do comprador", "presenca do comprador")),
                ParsingUtils.blankToNull(pickValue(identificacao, "modelo")),
                ParsingUtils.blankToNull(pickValue(identificacao, "série", "serie")),
                ParsingUtils.blankToNull(pickValue(identificacao, "número", "numero")),
                ParsingUtils.blankToNull(pickValue(identificacao, "data emissão", "data emissao")),
                ParsingUtils.parseMoney(pickValue(valores, "valor total do serviço", "valor total do servico")),
                ParsingUtils.parseMoney(pickValue(valores, "base de cálculo icms", "base de calculo icms")),
                ParsingUtils.parseMoney(pickValue(valores, "valor icms")),
                ParsingUtils.blankToNull(pickValue(protocolo, "protocolo"))
        );
    }

    /**
     * Faz o parse da secao de totais e usa os itens como fallback quando necessario.
     */
    private static Totais parseTotais(Document doc, NotaInfo nota, List<Item> itens) {
        Integer qtd = ParsingUtils.parseInteger(extractSummaryValue(doc, "Qtde total de ítens"));
        if (qtd == null && itens != null && !itens.isEmpty()) {
            qtd = itens.size();
        }

        BigDecimal valorTotal = ParsingUtils.parseMoney(extractSummaryValue(doc, "Valor total R$"));
        if (valorTotal == null && nota != null && nota.valorTotalServico() != null) {
            valorTotal = nota.valorTotalServico();
        }
        if (valorTotal == null && itens != null && !itens.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            boolean allHaveValue = true;
            for (Item item : itens) {
                if (item.valorTotal() == null) {
                    allHaveValue = false;
                    break;
                }
                sum = sum.add(item.valorTotal());
            }
            if (allHaveValue) {
                valorTotal = sum;
            }
        }

        return new Totais(qtd, valorTotal);
    }

    /**
     * Localiza o texto associado a um rotulo de resumo dentro de uma linha de elementos em negrito.
     */
    private static String extractSummaryValue(Document doc, String label) {
        String labelKey = ParsingUtils.normalizeKey(label);
        for (Element strong : doc.select("strong")) {
            if (!Objects.equals(ParsingUtils.normalizeKey(strong.text()), labelKey)) {
                continue;
            }
            Element row = strong.closest("div.row");
            if (row == null) {
                continue;
            }
            List<String> strongTexts = row.select("strong").eachText();
            if (strongTexts.size() >= 2) {
                return strongTexts.get(1);
            }
        }
        return null;
    }

    /**
     * Faz o parse da tabela de itens em uma lista de registros de item.
     */
    private static List<Item> parseItens(Document doc) {
        Element tbody = doc.getElementById("myTable");
        if (tbody == null) {
            return List.of();
        }

        List<Element> rows = tbody.select("tr");
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Item> itens = new ArrayList<>(rows.size());
        parseItemRowsRecursively(rows, 0, itens);
        return itens;
    }

    // Recursivo: processa o índice atual e chama o próximo até acabar.
    /**
     * Percorre as linhas de itens recursivamente para transformar cada linha em um item.
     */
    private static void parseItemRowsRecursively(List<Element> rows, int index, List<Item> out) {
        if (index >= rows.size()) {
            return;
        }
        out.add(parseItemRow(rows.get(index), index + 1));
        parseItemRowsRecursively(rows, index + 1, out);
    }

    /**
     * Extrai uma linha de item, incluindo descricao, codigo, quantidade, unidade e valor total.
     */
    private static Item parseItemRow(Element row, int numero) {
        Elements tds = row.select("> td");
        if (tds.size() < 4) {
            return new Item(numero, null, null, null, null, null);
        }

        Element td0 = tds.get(0);
        String descricao = null;
        Element h7 = td0.selectFirst("h7");
        if (h7 != null) {
            descricao = ParsingUtils.blankToNull(h7.text());
        }
        if (descricao == null) {
            String raw = td0.text();
            int idx = raw.toLowerCase().indexOf("(código");
            descricao = idx > 0 ? raw.substring(0, idx).trim() : raw.trim();
            descricao = ParsingUtils.blankToNull(descricao);
        }

        String codigo = null;
        String td0Text = td0.text();
        Matcher m = Pattern.compile("c[oó]digo:\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(td0Text);
        if (m.find()) {
            codigo = m.group(1);
        }

        BigDecimal quantidade = ParsingUtils.parseQuantity(tds.get(1).text());

        String unidade = null;
        String td2 = tds.get(2).text();
        int colon = td2.indexOf(':');
        unidade = colon >= 0 ? td2.substring(colon + 1).trim() : td2.trim();
        unidade = ParsingUtils.blankToNull(unidade);

        BigDecimal valorTotal = ParsingUtils.parseMoney(tds.get(3).text());

        return new Item(numero, descricao, ParsingUtils.blankToNull(codigo), quantidade, unidade, valorTotal);
    }

    /**
     * Encontra uma tabela que contenha todos os cabecalhos esperados, mesmo com pequenas variacoes no HTML.
     */
    private static Element findTableByRequiredHeaders(Element scope, String... requiredHeaders) {
        if (scope == null) {
            return null;
        }

        List<String> requiredKeys = new ArrayList<>();
        for (String h : requiredHeaders) {
            requiredKeys.add(ParsingUtils.normalizeKey(h));
        }

        for (Element table : scope.select("table")) {
            List<String> headers = table.select("thead th").eachText();
            if (headers.isEmpty()) {
                continue;
            }

            List<String> headerKeys = headers.stream().map(ParsingUtils::normalizeKey).toList();
            boolean ok = true;
            for (String req : requiredKeys) {
                boolean found = false;
                for (String hk : headerKeys) {
                    if (hk.contains(req)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return table;
            }
        }
        return null;
    }

    /**
     * Converte uma tabela de uma linha em um mapa normalizado de cabecalho para valor.
     */
    private static Map<String, String> extractSingleRowTable(Element table) {
        if (table == null) {
            return Map.of();
        }

        List<String> headers = table.select("thead th").eachText();
        Element row = table.selectFirst("tbody tr");
        if (headers.isEmpty() || row == null) {
            return Map.of();
        }

        // IMPORTANTE: jsoup `eachText()` ignora elementos sem texto.
        // Para não desalinharmos header->valor, precisamos preservar a posição de cada <td>.
        Elements cells = row.select("td");
        List<String> values = new ArrayList<>(cells.size());
        for (Element cell : cells) {
            values.add(cell.text());
        }

        int n = Math.min(headers.size(), values.size());

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(ParsingUtils.normalizeKey(headers.get(i)), values.get(i));
        }
        return map;
    }

    /**
     * Tenta varios possiveis cabecalhos e retorna o primeiro valor que bater.
     */
    private static String pickValue(Map<String, String> normalizedHeaderToValue, String... possibleHeaders) {
        if (normalizedHeaderToValue == null || normalizedHeaderToValue.isEmpty()) {
            return null;
        }
        for (String h : possibleHeaders) {
            String key = ParsingUtils.normalizeKey(h);
            String value = normalizedHeaderToValue.get(key);
            if (value != null) {
                return value;
            }
            // fallback por contains
            for (Map.Entry<String, String> entry : normalizedHeaderToValue.entrySet()) {
                if (entry.getKey().contains(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
