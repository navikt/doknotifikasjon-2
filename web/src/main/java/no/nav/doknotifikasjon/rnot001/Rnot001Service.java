package no.nav.doknotifikasjon.rnot001;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.exception.functional.NotifikasjonIkkeFunnetException;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.doknotifikasjon.rnot001.Rnot001Mapper.mapNotifikasjon;
import static no.nav.doknotifikasjon.utils.MDCUtils.handleMdc;

@Slf4j
@Component
public class Rnot001Service {
	private final NotifikasjonRepository notifikasjonRepository;

	public Rnot001Service(NotifikasjonRepository notifikasjonRepository) {
		this.notifikasjonRepository = notifikasjonRepository;
	}

	@Transactional(readOnly = true)
	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId) {
		try {
			handleMdc();
			Notifikasjon notifikasjon = notifikasjonRepository.findByBestillingsId(bestillingsId).orElseThrow(
					() -> {
						log.info(String.format("Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId));
						throw new NotifikasjonIkkeFunnetException(String.format(
								"Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId)
						);
					});
			return mapNotifikasjon(notifikasjon);
		} finally {
			MDC.clear();
		}
	}

}
