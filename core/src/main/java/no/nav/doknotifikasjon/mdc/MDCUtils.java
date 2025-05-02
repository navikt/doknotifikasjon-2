package no.nav.doknotifikasjon.mdc;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static no.nav.doknotifikasjon.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doknotifikasjon.constants.MDCConstants.NAV_CALL_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class MDCUtils {

	public static void handleMdc() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		MDC.put(MDC_CALL_ID, determineCallId(request.getHeader(NAV_CALL_ID)));
	}

	private static String determineCallId(String requestCallId) {
		return isEmpty(requestCallId) ? UUID.randomUUID().toString() : requestCallId;
	}

}
