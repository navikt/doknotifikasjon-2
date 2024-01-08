package no.nav.doknotifikasjon.config.cxf;

import no.nav.doknotifikasjon.config.properties.AltinnProps;
import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

public record AltinnClientCallBackHandler(AltinnProps altinnProps) implements CallbackHandler {
	@Override
	public void handle(Callback[] callbacks) {
		WSPasswordCallback wsPasswordCallback = (WSPasswordCallback) callbacks[0];
		wsPasswordCallback.setPassword(altinnProps.password());
	}
}
