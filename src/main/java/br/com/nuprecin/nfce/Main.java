package br.com.nuprecin.nfce;

import br.com.nuprecin.nfce.model.Nfce;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class Main {
    /**
     * Ponto de entrada da aplicacao.
     * Lê a URL do QR Code, consulta a pagina da NFC-e e imprime o JSON gerado.
     */
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : System.getenv("NFC_E_QRCODE_URL");

        if (url == null || url.isBlank()) {
            System.err.println("Uso: mvn -q exec:java -Dexec.args=\"<URL_QRCODE_NFCE>\"");
            System.err.println("Ou defina a env NFC_E_QRCODE_URL.");
            System.exit(2);
        }

        Nfce nfce = new NfceScraper().scrape(url);

        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println(mapper.writeValueAsString(nfce));
    }
}
