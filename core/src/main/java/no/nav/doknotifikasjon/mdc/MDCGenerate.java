package no.nav.doknotifikasjon.mdc;

import no.nav.doknotifikasjon.constants.MDCConstants;
import org.slf4j.MDC;

import java.util.UUID;

public class MDCGenerate {
	public static void generateNewCallIdIfThereAreNone(String key) {
		if (MDC.get(MDCConstants.MDC_CALL_ID) == null) {
			MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString());
		}
	}

	public static void clearCallId() {
		if (MDC.get(MDCConstants.MDC_CALL_ID) != null) {
			MDC.remove(MDCConstants.MDC_CALL_ID);
		}
	}

	public static void setDistribusjonId(String distribusjonId) {
		if (MDC.get(MDCConstants.DISTRIBUSJON_ID) != null) {
			MDC.put(MDCConstants.DISTRIBUSJON_ID, distribusjonId);
		}
	}

	public static void clearDistribusjonId() {
		if (MDC.get(MDCConstants.DISTRIBUSJON_ID) != null) {
			MDC.remove(MDCConstants.DISTRIBUSJON_ID);
		}
	}

	public static void clearMdc() {
		MDC.clear();
	}

	private MDCGenerate(){}
}
