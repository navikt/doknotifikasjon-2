package no.nav.doknotifikasjon.repository;

import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.exception.functional.DoknotifikasjonValidationException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.repository.utils.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.entity.ContentType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {ApplicationTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext()
@ActiveProfiles({"itest", "itestWiremock"})
public class DigitalKontaktinfoConsumerTest {

    private static final String MOBIL = "+4799999999";
    private static final String EPOST = "epost";

    @Autowired
    private DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

    @BeforeEach
    public void setup(){
        stubSecurityToken();
    }

    @Test
    void shouldReturnKontaktinfoWhenFullResponseFromDkif(){
        stubDkifWithBodyFile("dkif/dkif-full-response.json");

        DigitalKontaktinformasjonTo.DigitalKontaktinfo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo("12345678911");

        assertEquals(EPOST, digitalKontaktinfo.getEpostadresse());
        assertEquals(MOBIL, digitalKontaktinfo.getMobiltelefonnummer());
        assertTrue(digitalKontaktinfo.isKanVarsles());
        assertFalse(digitalKontaktinfo.isReservert());
    }

    @Test
    void shouldReturnKontaktinfoWhenOnlyKontaktinfoFromDkif(){
        stubDkifWithBodyFile("dkif/dkif-kontaktinfo-response.json");

        DigitalKontaktinformasjonTo.DigitalKontaktinfo digitalKontaktinfo = digitalKontaktinfoConsumer.hentDigitalKontaktinfo("12345678911");

        assertEquals(EPOST, digitalKontaktinfo.getEpostadresse());
        assertEquals(MOBIL, digitalKontaktinfo.getMobiltelefonnummer());
        assertTrue(digitalKontaktinfo.isKanVarsles());
        assertFalse(digitalKontaktinfo.isReservert());
    }

    @Test
    void shouldReturnNullWhenFeilFromDkif(){
        stubDkifWithBodyFile("dkif/dkif-feil.json");
        assertNull(digitalKontaktinfoConsumer.hentDigitalKontaktinfo("12345678911"));
    }

    @Test
    void shouldThrowErrorWhenNoResponseFromDkif(){
        stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        DigitalKontaktinformasjonTechnicalException exception = assertThrows(DigitalKontaktinformasjonTechnicalException.class, () ->
                digitalKontaktinfoConsumer.hentDigitalKontaktinfo("12345678911"));
        assertEquals("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=500 Server Error: [no body]", exception.getMessage());
    }

    private void stubSecurityToken() {
        stubFor(get("/sts?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("sts-response.json")));
    }

    private void stubDkifWithBodyFile(String bodyfile) {
        stubFor(get(urlEqualTo("/dkif/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                        .withBodyFile(bodyfile)));
    }
}
