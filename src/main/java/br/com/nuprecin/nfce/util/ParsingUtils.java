package br.com.nuprecin.nfce.util;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParsingUtils {
    private static final Pattern FIRST_NUMBER_TOKEN = Pattern.compile("-?\\d[\\d\\.,]*");

    /**
     * Classe utilitaria; nao sao necessarias instancias.
     */
    private ParsingUtils() {
    }

    /**
     * Converte strings em branco para null para manter o modelo limpo.
     */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Mantem apenas caracteres numericos em uma string.
     */
    public static String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    /**
     * Normaliza rotulos removendo acentos, colocando em minusculo e colapsando espacos.
     */
    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /**
     * Extrai o primeiro token com cara de inteiro de um texto arbitrario.
     */
    public static Integer parseInteger(String text) {
        String token = extractFirstNumberToken(text);
        if (token == null) {
            return null;
        }
        String digits = token.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Faz o parse de um valor monetario aceitando formato brasileiro ou decimal simples.
     */
    public static BigDecimal parseMoney(String text) {
        String token = extractFirstNumberToken(text);
        if (token == null) {
            return null;
        }
        String cleaned = token;
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "");
            cleaned = cleaned.replace(",", ".");
        }
        cleaned = cleaned.replaceAll("[^0-9.-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Faz o parse de uma quantidade usando a mesma tolerancia numerica do valor monetario.
     */
    public static BigDecimal parseQuantity(String text) {
        String token = extractFirstNumberToken(text);
        if (token == null) {
            return null;
        }
        String cleaned = token;
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "");
            cleaned = cleaned.replace(",", ".");
        }
        cleaned = cleaned.replaceAll("[^0-9.-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Monta uma URI a partir da URL do QR Code, codificando pipes invalidos quando necessario.
     */
    public static URI toLenientUri(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL nula");
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("URL vazia");
        }

        try {
            return URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            // QRCode NFC-e (MG) costuma trazer '|' na query do parâmetro p, que é inválido
            // para java.net.URI. Encode mínimo para permitir a requisição.
            String sanitized = trimmed.replace("|", "%7C");
            return URI.create(sanitized);
        }
    }

    /**
     * Extrai a chave de acesso de 44 digitos a partir do parametro `p` da URL do QR Code.
     */
    public static String extractChaveAcessoFromUrl(String qrcodeUrl) {
        if (qrcodeUrl == null || qrcodeUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = toLenientUri(qrcodeUrl);
            String rawQuery = uri.getRawQuery();
            if (rawQuery == null || rawQuery.isBlank()) {
                return null;
            }

            for (String part : rawQuery.split("&")) {
                int idx = part.indexOf('=');
                String key = idx >= 0 ? part.substring(0, idx) : part;
                if (!"p".equalsIgnoreCase(key)) {
                    continue;
                }
                String rawValue = idx >= 0 ? part.substring(idx + 1) : "";
                String decoded = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                String[] pieces = decoded.split("\\|");
                if (pieces.length == 0) {
                    return null;
                }
                String first = digitsOnly(pieces[0]);
                if (first != null && first.length() == 44) {
                    return first;
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Recupera o primeiro token numerico do texto para reutilizar o parse numerico.
     */
    private static String extractFirstNumberToken(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = FIRST_NUMBER_TOKEN.matcher(text);
        return m.find() ? m.group() : null;
    }
}
