package no.nav.doknotifikasjon.mdc;

import org.slf4j.MDC;
import java.util.UUID;

import static no.nav.doknotifikasjon.constants.MDCConstants.DISTRIBUSJON_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;

public class MDCGenerate {
	public static void generateNewCallIdIfThereAreNone(String uuid) {
		if (uuid != null) {
			MDC.put(MDC_CALL_ID, uuid);
		} else if (MDC.get(MDC_CALL_ID) == null) {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
		}
	}

	public static void clearCallId() {
		if (MDC.get(MDC_CALL_ID) != null) {
			MDC.remove(MDC_CALL_ID);
		}
	}

	public static void setDistribusjonId(String distribusjonId) {
		if (distribusjonId != null) {
			MDC.put(DISTRIBUSJON_ID, distribusjonId);
		}
	}

	public static void clearDistribusjonId() {
		if (MDC.get(DISTRIBUSJON_ID) != null) {
			MDC.remove(DISTRIBUSJON_ID);
		}
	}

	public static String getDefaultUuidIfNoCallIdIsSett() {
		if (MDC.get(MDC_CALL_ID) != null && !MDC.get(MDC_CALL_ID).isBlank()) {
			return MDC.get(MDC_CALL_ID);
		}
		return UUID.randomUUID().toString();
	}

	private MDCGenerate(){}
}
