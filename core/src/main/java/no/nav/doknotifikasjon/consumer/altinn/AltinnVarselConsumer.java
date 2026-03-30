package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.kodeverk.Kanal;

import java.util.Optional;
import java.util.UUID;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static no.nav.doknotifikasjon.kodeverk.Kanal.SMS;

public interface AltinnVarselConsumer {

	Optional<UUID> sendVarsel(Kanal kanal, String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel);

	default Optional<UUID> sendEpostVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel) {
		return sendVarsel(EPOST, bestillingsId, kontaktInfo, fnr, tekst, tittel);
	}
	default Optional<UUID> sendSmsVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst) {
		return sendVarsel(SMS, bestillingsId, kontaktInfo, fnr, tekst, "");
	}
}
