package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinformasjonTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class Rnot002Service {

	private final DigitalKontaktinfoConsumer digdirConsumer;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;

	public Rnot002Service(DigitalKontaktinfoConsumer digdirConsumer,
						  SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer) {
		this.digdirConsumer = digdirConsumer;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
	}

	public KanVarslesResponse getKanVarsles(String personident) {

		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digdirConsumer.hentDigitalKontaktinfo(personident);

		if (kanIkkeVarsles(digitalKontaktinformasjonTo, personident)) {
			return new KanVarslesResponse(false, 3);
		}

		AuthLevelResponse authLevelResponse = sikkerhetsnivaaConsumer.lookupAuthLevel(personident);

		return new KanVarslesResponse(true, authLevelResponse.isHarbruktnivaa4()? 4 : 3);
	}

	private boolean kanIkkeVarsles(DigitalKontaktinformasjonTo digitalKontaktinformasjonTo, String personident) {

		if (digitalKontaktinformasjonTo.feil() != null && digitalKontaktinformasjonTo.feil().get(personident) != null) {
			return true;
		} else if (digitalKontaktinformasjonTo.personer() != null && digitalKontaktinformasjonTo.personer().get(personident) != null) {
			var digitalKontaktInfo = digitalKontaktinformasjonTo.personer().get(personident);

			return !digitalKontaktInfo.kanVarsles() ||
					digitalKontaktInfo.reservert() ||
					(isBlank(digitalKontaktInfo.epostadresse()) && isBlank(digitalKontaktInfo.mobiltelefonnummer()));
		}
		return false;
	}
}
