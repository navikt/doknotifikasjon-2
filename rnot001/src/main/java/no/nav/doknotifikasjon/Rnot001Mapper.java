package no.nav.doknotifikasjon;

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
				notifikasjon.getNotifikasjonDistribusjon().stream().map(notifikasjonDistribusjon ->
						mapNotifikasjonDistribusjon(notifikasjonDistribusjon)).collect(Collectors.toList())
		);
	}

	private static NotifikasjonDistribusjonDto mapNotifikasjonDistribusjon(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return new NotifikasjonDistribusjonDto(
				notifikasjonDistribusjon.getId(),
				notifikasjonDistribusjon.getStatus(),
				notifikasjonDistribusjon.getKanal(),
				notifikasjonDistribusjon.getKontaktInfo(),
				mapTittel(notifikasjonDistribusjon),
				notifikasjonDistribusjon.getTekst(),
				notifikasjonDistribusjon.getSendtDato()
		);
	}

	private static String mapTittel(NotifikasjonDistribusjon notifikasjonDistribusjon) {
		return EPOST.equals(notifikasjonDistribusjon.getKanal()) ? notifikasjonDistribusjon.getTittel() : null;
	}

}
