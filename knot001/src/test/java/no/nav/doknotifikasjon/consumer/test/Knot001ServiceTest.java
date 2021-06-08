package no.nav.doknotifikasjon.consumer.test;

import no.nav.doknotifikasjon.consumer.DoknotifikasjonTO;
import no.nav.doknotifikasjon.consumer.Knot001Service;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.dkif.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.doknotifikasjon.exception.functional.DuplicateNotifikasjonInDBException;
import no.nav.doknotifikasjon.exception.functional.KontaktInfoValidationFunctionalException;
import no.nav.doknotifikasjon.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.doknotifikasjon.kafka.KafkaEventProducer;
import no.nav.doknotifikasjon.kafka.KafkaStatusEventProducer;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static no.nav.doknotifikasjon.consumer.TestUtils.FODSELSNUMMER;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDigitalKontaktinformasjonInfo;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDigitalKontaktinformasjonInfoWithErrorMessage;
import static no.nav.doknotifikasjon.consumer.TestUtils.createDoknotifikasjonTO;
import static no.nav.doknotifikasjon.consumer.TestUtils.createEmptyDigitalKontaktinformasjonInfo;
import static no.nav.doknotifikasjon.consumer.TestUtils.createInvalidKontaktInfo;
import static no.nav.doknotifikasjon.consumer.TestUtils.createInvalidKontaktInfoWithoutKontaktInfo;
import static no.nav.doknotifikasjon.consumer.TestUtils.createValidKontaktInfo;
import static no.nav.doknotifikasjon.consumer.TestUtils.createValidKontaktInfoReserved;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT;
import static no.nav.doknotifikasjon.kafka.DoknotifikasjonStatusMessage.INFO_ALREADY_EXIST_IN_DATABASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {Knot001Service.class})
class Knot001ServiceTest {

	@Autowired
	Knot001Service knot001Service;

	@MockBean
	KafkaStatusEventProducer statusProducer;

	@MockBean
	DigitalKontaktinfoConsumer digitalKontaktinfoConsumer;

	@MockBean
	NotifikasjonService notifikasjonService;

	@MockBean
	KafkaEventProducer producer;

	@MockBean
	SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;

	@Test
	void ShouldGetValidKontaktInfoWhenSendingWithValidFnr() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createDigitalKontaktinformasjonInfo());

		DigitalKontaktinformasjonTo.DigitalKontaktinfo kontaktinfo = knot001Service.getKontaktInfoByFnr(createDoknotifikasjonTO());
		DigitalKontaktinformasjonTo.DigitalKontaktinfo validKontaktInfo = createValidKontaktInfo();

		assertEquals(validKontaktInfo.getEpostadresse(), kontaktinfo.getEpostadresse());
		assertEquals(validKontaktInfo.getMobiltelefonnummer(), kontaktinfo.getMobiltelefonnummer());
		assertEquals(validKontaktInfo.isKanVarsles(), kontaktinfo.isKanVarsles());
		assertEquals(validKontaktInfo.isReservert(), kontaktinfo.isReservert());
	}

	@Test
	void ShouldGetExceptionWhenDigitalKontaktinfoConsumerThrows() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenThrow(new DigitalKontaktinformasjonTechnicalException(""));
		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		assertThrows(DigitalKontaktinformasjonTechnicalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));
	}

	@Test
	void ShouldGetExceptionWhenRequestingKontaktInfoWithInvalidFnr() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createEmptyDigitalKontaktinformasjonInfo());
		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		assertThrows(KontaktInfoValidationFunctionalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));

		verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET, null
		);
	}

	@Test
	void ShouldGetExceptionWhenRequestingKontaktInfoWithErrorMessage() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createDigitalKontaktinformasjonInfoWithErrorMessage());

		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();

		assertThrows(KontaktInfoValidationFunctionalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));

		verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), "Ingen kontaktinformasjon er registrert på personen", null
		);
	}

	@Test
	void ShouldGetExceptionWhenRequestingKontaktInfoWithReserved() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createValidKontaktInfoReserved());

		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		assertThrows(KontaktInfoValidationFunctionalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));

		verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT, null
		);
	}

	@Test
	void ShouldGetExceptionWhenRequestingKontaktInfoWithVarselFalse() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createInvalidKontaktInfo());

		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		assertThrows(KontaktInfoValidationFunctionalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));

		verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION, null
		);
	}

	@Test
	void ShouldGetExceptionWhenRequestingKontaktInfoWithMissingEmailAndSms() {
		when(digitalKontaktinfoConsumer.hentDigitalKontaktinfo(FODSELSNUMMER))
				.thenReturn(createInvalidKontaktInfoWithoutKontaktInfo());

		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		assertThrows(KontaktInfoValidationFunctionalException.class, () -> knot001Service.getKontaktInfoByFnr(doknotifikasjon));

		verify(statusProducer).publishDoknotikfikasjonStatusFeilet(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION, null
		);
	}

	@Test
	void shouldGetExceptionWhenNotifikasjonWithTheSameBestillingsIdAlreadyExist() {
		DoknotifikasjonTO doknotifikasjon = createDoknotifikasjonTO();
		DigitalKontaktinformasjonTo.DigitalKontaktinfo digitalKontaktinfo = createValidKontaktInfo();

		when(notifikasjonService.existsByBestillingsId(anyString()))
				.thenReturn(true);

		assertThrows(DuplicateNotifikasjonInDBException.class, () ->
				knot001Service.createNotifikasjonByDoknotifikasjonTO(doknotifikasjon, digitalKontaktinfo)
		);

		verify(statusProducer).publishDoknotikfikasjonStatusInfo(
				doknotifikasjon.getBestillingsId(), doknotifikasjon.getBestillerId(), INFO_ALREADY_EXIST_IN_DATABASE, null
		);
	}
}