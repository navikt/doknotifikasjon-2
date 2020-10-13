package no.nav.doknotifikasjon;

import lombok.Builder;
import lombok.Value;

@Builder
public class DoknotifikasjonStatusDto {
	String bestillingId;
	String bestillerId;
	String status;
	String melding;
	Long distribusjonId;
}
