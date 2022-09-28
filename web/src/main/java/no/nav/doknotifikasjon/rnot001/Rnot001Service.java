package no.nav.doknotifikasjon.rnot001;

import no.nav.doknotifikasjon.exception.functional.NotifikasjonIkkeFunnetException;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonRepository;
import no.nav.doknotifikasjon.repository.NotifikasjonDistribusjonService;

import java.util.List;

public class Rnot001Service {
	private final NotifikasjonDistribusjonRepository notifikasjonDistribusjonRepository;
	private final Rnot001Mapper rnot001Mapper;

	public Rnot001Service(NotifikasjonDistribusjonService notifikasjonDistribusjonService,
						  NotifikasjonDistribusjonRepository notifikasjonDistribusjonService1, Rnot001Mapper rnot001Mapper) {
		this.notifikasjonDistribusjonRepository = notifikasjonDistribusjonService1;
		this.rnot001Mapper = rnot001Mapper;
	}

	public NotifikasjonInfoTo getNotifikasjonInfo(String bestillingsId){
		List<NotifikasjonDistribusjon> notifikasjonDistribusjoner = notifikasjonDistribusjonRepository.findAllByNotifikasjon_BestillingsId(bestillingsId);
		if(notifikasjonDistribusjoner.isEmpty()){
			throw new NotifikasjonIkkeFunnetException(String.format("Notifikasjon med bestillingsId=%s ble ikke funnet i databasen.", bestillingsId));
		}
		return rnot001Mapper.mapNotifikasjonDistribusjon(notifikasjonDistribusjoner);
	}

}
