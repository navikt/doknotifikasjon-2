package no.nav.doknotifikasjon.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DigitalKontaktinoConsumer implements DigitalKontaktinformasjon{

//	private final String dkifUrl;
//	private final RestTemplate restTemplate;
//
//	@Inject
//	public DigitalKontaktinoConsumer(@Value("dkif_url") String dkifUrl, RestTemplateBuilder restTemplateBuilder){
//		this.dkifUrl = dkifUrl;
//		this.restTemplate = restTemplateBuilder
//				.setReadTimeout(Duration.ofSeconds(20))
//				.setConnectT
//	}

	public DigitalKontaktinformasjonTo hentDigitalKontaktinfo(final String personidentifikator){}
}
