package no.nav.doknotifikasjon.consumer.altinn;

import no.nav.doknotifikasjon.exception.technical.AltinnTechnicalException;
import no.nav.doknotifikasjon.kodeverk.Kanal;

public interface AltinnVarselConsumer {

	void sendVarsel(Kanal kanal, String kontaktInfo, String fnr, String tekst, String tittel);

	void altinnTechnicalExceptionRecovery(AltinnTechnicalException e);

	// Catch-all for alle andre exceptions - hvis ikke blir ExhaustedRetryException kastet med meldingen 'Cannot locate recovery method'
	void otherExceptionsRecovery(RuntimeException e);
}
