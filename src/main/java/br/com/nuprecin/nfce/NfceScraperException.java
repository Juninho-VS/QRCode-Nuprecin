package br.com.nuprecin.nfce;

public class NfceScraperException extends RuntimeException {
    public NfceScraperException(String message) {
        super(message);
    }

    public NfceScraperException(String message, Throwable cause) {
        super(message, cause);
    }
}
