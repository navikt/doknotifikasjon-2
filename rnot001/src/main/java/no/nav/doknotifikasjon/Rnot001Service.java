package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonIkkeFunnetException;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;
import static no.nav.doknotifikasjon.Rnot001Mapper.mapNotifikasjon;

@Slf4j
@Component
@Transactional(readOnly = true)
public class Rnot001Service {
	private final NotifikasjonRepository notifikasjonRepository;

	public Rnot001Service(NotifikasjonRepository notifikasjonRepository) {
		this.notifikasjonRepository = notifikasjonRepository;
	}

	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId) {

		Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(bestillingsId)
				.orElseThrow(() -> {
					log.info("Notifikasjon med bestillingsId={} ble ikke funnet i databasen.", bestillingsId);

					return new NotifikasjonIkkeFunnetException(format("Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId));
				});

		return mapNotifikasjon(notifikasjon);
	}

}
