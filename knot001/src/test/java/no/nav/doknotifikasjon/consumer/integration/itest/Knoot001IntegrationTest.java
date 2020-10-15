package no.nav.doknotifikasjon.consumer.integration.itest;

import no.nav.doknotifikasjon.KafkaProducer.KafkaEventProducer;
import no.nav.doknotifikasjon.consumer.integration.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.utils.KafkaTopics.KAFKA_TOPIC_DOK_NOTIFKASJON;

public class Knoot001IntegrationTest extends EmbededKafkaBroker{

    @Autowired
    KafkaEventProducer KafkaEventProducer;

    @MockBean
    DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

//    @BeforeAll
//    public void beforeAll() {
//        when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(anyString()))
//                .thenReturn(this.createValidKontaktInfo());
//    }

    @Test
    public void TestPosetiveConsumer() throws InterruptedException {
        Doknotifikasjon dokEksternNotifikasjon = new Doknotifikasjon(
                "bestillingsId",
                "bestillerId",
                "fodselsnummer",
                0,
                0,
                "tittel",
                "epostTekst",
                "smsTekst",
                "prefererteKanaler"
        );

        Long keyGenerator = System.currentTimeMillis();

        KafkaEventProducer.publish(
                KAFKA_TOPIC_DOK_NOTIFKASJON,
                dokEksternNotifikasjon,
                keyGenerator
        );

        TimeUnit.SECONDS.sleep(30);
    }



    public DigitalKontaktinformasjonTo.DigitalKontaktinfo createValidKontaktInfo() {
        return DigitalKontaktinformasjonTo.DigitalKontaktinfo.builder()
                .epostadresse("bogus")
                .mobiltelefonnummer("bogus")
                .kanVarsles(true)
                .reservert(false)
                .build();
    }
}
