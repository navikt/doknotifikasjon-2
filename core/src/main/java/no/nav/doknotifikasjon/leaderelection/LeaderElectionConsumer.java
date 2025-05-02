package no.nav.doknotifikasjon.leaderelection;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.time.Duration;

import static java.lang.String.format;

@Slf4j
@Component
public class LeaderElectionConsumer implements LeaderElection {

	private static final String ELECTOR_PATH = "ELECTOR_PATH";

	private final RestTemplate restTemplate;
	private final ObjectMapper mapper;

	public LeaderElectionConsumer(RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
		this.restTemplate = restTemplateBuilder
				.readTimeout(Duration.ofSeconds(20))
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.mapper = mapper;
	}

	@Override
	public boolean isLeader() {
		String electorPath = System.getenv(ELECTOR_PATH);

		if (electorPath == null) {
			log.warn("Kunne ikke bestemme lederpod på grunn av manglende systemvariabel={}.", ELECTOR_PATH);
			return true;
		}

		try {
			String response = restTemplate.getForObject("http://" + electorPath, String.class);
			String leader = mapper.readTree(response).get("name").asText();
			String hostname = InetAddress.getLocalHost().getHostName();
			return hostname.equals(leader);
		} catch (Exception e) {
			log.warn(format("Kunne ikke bestemme lederpod. Feilmelding: %s", e.getMessage()), e);
			return true;
		}
	}
}
