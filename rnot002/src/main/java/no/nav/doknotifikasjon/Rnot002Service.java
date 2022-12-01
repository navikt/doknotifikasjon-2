package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.DigitalKontaktinfoConsumer;
import no.nav.doknotifikasjon.consumer.digdir.krr.proxy.KontaktinfoTo;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.AuthLevelResponse;
import no.nav.doknotifikasjon.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.doknotifikasjon.exception.functional.DigitalKontaktinformasjonFunctionalException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static no.nav.doknotifikasjon.Kontaktinfo.personKanVarsles;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class Rnot002Service {

	private final DigitalKontaktinfoConsumer digdirConsumer;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;
	private static final EnumSet<HttpStatus> PERSON_IKKE_TILGJENGELIG = EnumSet.of(FORBIDDEN, NOT_FOUND);

	public Rnot002Service(DigitalKontaktinfoConsumer digdirConsumer,
						  SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer) {
		this.digdirConsumer = digdirConsumer;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
	}

	public KanVarslesResponse getKanVarsles(String personident) {

		KontaktinfoTo kontaktinfoTo;
		try {
			kontaktinfoTo = digdirConsumer.hentDigitalKontaktinfoForPerson(personident);
		} catch (DigitalKontaktinformasjonFunctionalException e) {
			if (PERSON_IKKE_TILGJENGELIG.contains(e.getHttpStatus())) {
				return new KanVarslesResponse(false, 3);
			}
			throw e;
		}

		Kontaktinfo kontaktinfo = Kontaktinfo.from(kontaktinfoTo);

		if (!personKanVarsles(kontaktinfo)) {
			return new KanVarslesResponse(false, 3);
		}

		AuthLevelResponse authLevelResponse = sikkerhetsnivaaConsumer.lookupAuthLevel(personident);

		return new KanVarslesResponse(true, authLevelResponse.isHarbruktnivaa4() ? 4 : 3);
	}
}
