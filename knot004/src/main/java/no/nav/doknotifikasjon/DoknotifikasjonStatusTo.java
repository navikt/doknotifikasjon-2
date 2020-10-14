package no.nav.doknotifikasjon;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class DoknotifikasjonStatusTo {
	String bestillingId;
	String bestillerId;
	String status;
	String melding;
	Long distribusjonId;
}
