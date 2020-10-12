package no.nav.doknotifikasjon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DokEksternnotifikasjonStatusEvent {
	//	String operation;
	String bestillingId;
	String bestillerId;
	String status;
	String melding;
	int distribusjonId;
	//	Set<String> columnsChanged;
//	Long operationTimestamp;
//	Long currentTimestamp;
}
