package no.nav.doknotifikasjon.consumer;

import no.nav.doknotifikasjon.KafkaProducer.KafkaDoknotifikasjonStatusProducer;
import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.service.NotifikasjonDistrbusjonService;
import no.nav.doknotifikasjon.service.NotifikasjonService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;

class DoknotifikasjonServiceTest {

    public static String validFnr = "123456789012345";


    @Autowired
    DoknotifikasjonService doknotifikasjonService;

    @MockBean
    KafkaDoknotifikasjonStatusProducer StatusProducer;

    @MockBean
    NotifikasjonService notifikasjonService;

    @MockBean
    NotifikasjonDistrbusjonService notifikasjonDistrbusjonService;

    @MockBean
    KafkaEventProducer producer;

    @MockBean
    DigitalKontaktinfoConsumer kontaktinfoConsumer;


    @BeforeAll
    public void beforeAll() {
        when(kontaktinfoConsumer.hentDigitalKontaktinfo(validFnr))
                .thenReturn(this.createValidKontaktInfo());
    }

    @Test
    void getKontaktInfoByFnr() {
        DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = doknotifikasjonService.getKontaktInfoByFnr(createDoknotifikasjon());
        DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo2 = createValidKontaktInfo();
    }

    @Test
    void publishDoknotikfikasjonStatusDKIF() {
    }

    @Test
    void createNotifikasjonFromDoknotifikasjon() {
    }

    @Test
    void createNotifikasjonByDoknotikasjon() {
    }

    @Test
    void createNotifikasjonDistrubisjon() {
    }

    @Test
    void validateAvroDoknotifikasjon() {
    }

    @Test
    void validateString() {
    }

    @Test
    void validateNumber() {
    }

    @Test
    void publishDoknotikfikasjonSms() {
    }

    @Test
    void publishDoknotikfikasjonEpost() {
    }








    public DigitalKontaktinformasjonTo.DigitalKontaktinfo createValidKontaktInfo() {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .epostadresse("bogus")
                .mobiltelefonnummer("bogus")
                .kanVarsles(true)
                .reservert(false)
                .build();
    }

    public DigitalKontaktinformasjonTo.DigitalKontaktinfo createInvalidKontaktInfoWithoutKontaktInfo() {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .kanVarsles(true)
                .reservert(false)
                .build();
    }

    public DigitalKontaktinformasjonTo.DigitalKontaktinfo createValidKontaktInfoReserved() {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .epostadresse("bogus")
                .mobiltelefonnummer("bogus")
                .kanVarsles(true)
                .reservert(true)
                .build();
    }

    public DigitalKontaktinformasjonTo.DigitalKontaktinfo createInvalidKontaktInfo() {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .epostadresse("bogus")
                .mobiltelefonnummer("bogus")
                .kanVarsles(false)
                .reservert(true)
                .build();
    }

    public Doknotifikasjon createDoknotifikasjon() {
        return new Doknotifikasjon(
                "bestillingsId",
                "bestillerId",
                validFnr,
                0,
                0,
                "tittel",
                "epostTekst",
                "smsTekst",
                "prefererteKanaler"
        );
    }
}