package br.com.nuprecin.nfce.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class ParsingUtilsTest {

    @Test
    void extractChaveAcessoFromUrl_deveSuportarPipeNaoEscapado() {
        String chave = "31260404641376013467650700002314271351124718";
        String url = "https://exemplo/qrcode.xhtml?p=" + chave + "|2|1|1|ABC";

        assertEquals(chave, ParsingUtils.extractChaveAcessoFromUrl(url));
    }

    @Test
    void toLenientUri_deveAceitarPipeNaoEscapado() {
        String chave = "31260404641376013467650700002314271351124718";
        URI uri = ParsingUtils.toLenientUri("https://exemplo/qrcode.xhtml?p=" + chave + "|2|1");

        assertEquals("p=" + chave + "%7C2%7C1", uri.getRawQuery());
    }
}
