package no.nav.doknotifikasjon.config;

import no.nav.doknotifikasjon.schemas.Doknotifikasjon;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonEpost;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonSms;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp;
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo;
import org.apache.avro.util.ClassSecurityValidator;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvroSecurityConfig {

	static {
		// Avro deserialiserer kun til klasser som er i en allowlist; alt som ikke er med her vil feile
		ClassSecurityValidator.setGlobal(ClassSecurityValidator.composite(
				ClassSecurityValidator.DEFAULT,
				ClassSecurityValidator.builder()
						.add(Doknotifikasjon.class)
						.add(DoknotifikasjonEpost.class)
						.add(DoknotifikasjonSms.class)
						.add(DoknotifikasjonStatus.class)
						.add(DoknotifikasjonStopp.class)
						.add(NotifikasjonMedkontaktInfo.class)
						.build()));
	}
}
