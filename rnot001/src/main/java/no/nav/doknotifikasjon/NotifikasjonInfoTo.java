package no.nav.doknotifikasjon;

import lombok.Value;
import no.nav.doknotifikasjon.kodeverk.Status;

import java.util.List;

@Value
public class NotifikasjonInfoTo {
	int id;
	String bestillerId;
	Status status;
	int antallRenotifikasjoner;
	List<NotifikasjonDistribusjonDto> notifikasjonDistribusjoner;
}
