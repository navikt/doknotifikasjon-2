package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.kodeverk.Kanal;

import java.util.Optional;
import java.util.UUID;

public interface AltinnVarselConsumer {

	Optional<UUID> sendVarsel(Kanal kanal, String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel);

	default Optional<UUID> sendEpostVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst, String tittel) {
		return sendVarsel(Kanal.EPOST, bestillingsId, kontaktInfo, fnr, tekst, tittel);
	}
	default Optional<UUID> sendSmsVarsel(String bestillingsId, String kontaktInfo, String fnr, String tekst) {
		return sendVarsel(Kanal.SMS, bestillingsId, kontaktInfo, fnr, tekst, "");
	}
}
