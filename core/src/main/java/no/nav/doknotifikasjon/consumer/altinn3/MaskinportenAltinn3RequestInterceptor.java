package no.nav.doknotifikasjon.consumer.altinn3;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class MaskinportenAltinn3RequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String ALTINN_SERVICEOWNER_SCOPE = "altinn:serviceowner";
	public static final String ALTINN_ORDER_INSTANT_MESSAGE_SCOPE = "altinn:serviceowner/notifications.create";

	private final Altinn3TokenExchangeConsumer altinn3TokenExchangeConsumer;

	public MaskinportenAltinn3RequestInterceptor(Altinn3TokenExchangeConsumer altinn3TokenExchangeConsumer) {
		this.altinn3TokenExchangeConsumer = altinn3TokenExchangeConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution next) throws IOException {
		request.getHeaders().setBearerAuth(
			altinn3TokenExchangeConsumer.getAltinnToken(ALTINN_SERVICEOWNER_SCOPE, ALTINN_ORDER_INSTANT_MESSAGE_SCOPE)
		);
		return next.execute(request, body);
	}
}
