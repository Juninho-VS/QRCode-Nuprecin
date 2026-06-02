package br.com.nuprecin.nfce;

/**
 * Sinaliza falhas ao baixar ou validar a pagina do QR Code da NFC-e.
 */
public class NfceScraperException extends RuntimeException {
    /**
     * Cria uma excecao com apenas uma mensagem.
     */
    public NfceScraperException(String message) {
        super(message);
    }

    /**
     * Cria uma excecao com mensagem e a causa original.
     */
    public NfceScraperException(String message, Throwable cause) {
        super(message, cause);
    }
}
