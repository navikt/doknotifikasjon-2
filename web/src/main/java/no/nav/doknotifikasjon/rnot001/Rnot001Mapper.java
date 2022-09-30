package no.nav.doknotifikasjon.rnot001;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import java.util.stream.Collectors;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;

public class Rnot001Mapper {

	public static NotifikasjonInfoTo mapNotifikasjon(Notifikasjon notifikasjon) {
		return new NotifikasjonInfoTo(
				notifikasjon.getId(),
				notifikasjon.getBestillerId(),
				notifikasjon.getStatus(),
				notifikasjon.getAntallRenotifikasjoner(),
				notifikasjon.getNotifikasjonDistribusjon().stream().map(notDist -> mapNotifikasjonDistribusjon(notDist)).collect(Collectors.toList())
		);
	}

	private static NotifikasjonDistribusjonDto mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notDist) {
		return new NotifikasjonDistribusjonDto(
				notDist.getId(),
				notDist.getStatus(),
				notDist.getKanal(),
				notDist.getKontaktInfo(),
				mapTittel(notDist),
				notDist.getTekst(),
				notDist.getSendtDato()
		);
	}

	private static String mapTittel(NotifikasjonDistribusjon notDist) {
		return EPOST.equals(notDist.getKanal()) ? notDist.getTittel() : null;
	}

}
