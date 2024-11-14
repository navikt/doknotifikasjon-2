package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;

public class Rnot001Mapper {

	public static NotifikasjonInfoTo mapNotifikasjon(Notifikasjon notifikasjon) {
		return new NotifikasjonInfoTo(
				notifikasjon.getId(),
				notifikasjon.getBestillerId(),
				notifikasjon.getStatus(),
				mapNullToZero(notifikasjon.getAntallRenotifikasjoner()),
				notifikasjon.getNotifikasjonDistribusjon().stream().map(Rnot001Mapper::mapNotifikasjonDistribusjon).toList()
		);
	}

	private static int mapNullToZero(Integer value) {
		return value == null ? 0 : value;
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
