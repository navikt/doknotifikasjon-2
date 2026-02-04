package no.nav.doknotifikasjon.consumer.altinn3;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class NaisTexasMaskinportenRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String ALTINN_SERVICEOWNER_SCOPE = "altinn:serviceowner";
	public static final String ALTINN_ORDER_INSTANT_MESSAGE_SCOPE = "altinn:serviceowner/notifications.create";

	private final NaisTexasConsumer naisTexasConsumer;

	public NaisTexasMaskinportenRequestInterceptor(NaisTexasConsumer naisTexasConsumer) {
		this.naisTexasConsumer = naisTexasConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution next) throws IOException {
		request.getHeaders().setBearerAuth(
			naisTexasConsumer.getMaskinportenToken(ALTINN_SERVICEOWNER_SCOPE, ALTINN_ORDER_INSTANT_MESSAGE_SCOPE)
		);
		return next.execute(request, body);
	}
}
