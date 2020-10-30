package no.nav.doknotifikasjon;

import lombok.Builder;
import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Status;

@Builder
@Value
public class DoknotifikasjonStatusTo {
	String bestillingsId;
	String bestillerId;
	Status status;
	String melding;
	Long distribusjonId;
}
