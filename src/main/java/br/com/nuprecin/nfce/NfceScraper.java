package br.com.nuprecin.nfce;

import br.com.nuprecin.nfce.model.Nfce;
import br.com.nuprecin.nfce.util.ParsingUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NfceScraper {
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String userAgent;

    /**
     * Cria um scraper com padroes sensatos para redirecionamento, timeout e user agent.
     */
    public NfceScraper() {
        this(HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(20))
                        .build(),
                Duration.ofSeconds(30),
                "Mozilla/5.0 (Java NFC-e Scraper)"
        );
    }

    /**
     * Cria um scraper com configuracao HTTP personalizada para testes ou usos especiais.
     */
    public NfceScraper(HttpClient httpClient, Duration requestTimeout, String userAgent) {
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
        this.userAgent = userAgent;
    }

    /**
     * Baixa a pagina do QR Code e envia o HTML para o parser.
     *
     * @param qrcodeUrl URL do QR Code da NFC-e
     * @return dados da NFC-e ja parseados
     * @throws NfceScraperException quando a requisicao falha ou a resposta e invalida
     */
    public Nfce scrape(String qrcodeUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(ParsingUtils.toLenientUri(qrcodeUrl))
                    .timeout(requestTimeout)
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                throw new NfceScraperException("Falha ao acessar QRCode: HTTP " + code);
            }

            String html = response.body();
            if (html == null || html.isBlank()) {
                throw new NfceScraperException("Resposta vazia ao acessar QRCode");
            }

            return NfceParser.parse(html, qrcodeUrl);
        } catch (IllegalArgumentException e) {
            throw new NfceScraperException("URL inválida: " + qrcodeUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NfceScraperException("Requisição interrompida", e);
        } catch (IOException e) {
            throw new NfceScraperException("Erro de IO ao acessar QRCode", e);
        }
    }
}
