package no.nav.doknotifikasjon;

import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import org.junit.jupiter.api.Test;

import static no.nav.doknotifikasjon.kodeverk.Status.FEILET;
import static no.nav.doknotifikasjon.kodeverk.Status.FERDIGSTILT;
import static no.nav.doknotifikasjon.kodeverk.Status.OPPRETTET;
import static no.nav.doknotifikasjon.kodeverk.Status.OVERSENDT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Knot004ServiceTest {

	Knot004Service knot004Service = new Knot004Service(null, null , null, null);

	@Test
	public void ShouldUpdateStatusOpprettetoOversendt() {
		Notifikasjon notifikasjon = new Notifikasjon();
		notifikasjon.setStatus(OPPRETTET);
		DoknotifikasjonStatusTo updateStatusTo = doknotifikasjonStatusBuilder(OVERSENDT);
		assertTrue(knot004Service.statusIsNewerThanPreviousStatus(
				updateStatusTo,
				notifikasjon
		));
	}

	@Test
	public void ShouldUpdateStatusOversendtToFerdigstilt() {
		Notifikasjon notifikasjon = new Notifikasjon();
		notifikasjon.setStatus(OVERSENDT);
		DoknotifikasjonStatusTo updateStatusTo = doknotifikasjonStatusBuilder(FERDIGSTILT);
		assertTrue(knot004Service.statusIsNewerThanPreviousStatus(
				updateStatusTo,
				notifikasjon
		));
	}

	@Test
	public void ShouldUpdateStatusFerdigstiltToFeilet() {
		Notifikasjon notifikasjon = new Notifikasjon();
		notifikasjon.setStatus(FERDIGSTILT);
		DoknotifikasjonStatusTo updateStatusTo = doknotifikasjonStatusBuilder(FEILET);
		assertTrue(knot004Service.statusIsNewerThanPreviousStatus(
				updateStatusTo,
				notifikasjon
		));
	}

	@Test
	public void ShouldNotUpdateStatusFerdigstiltToOversendt() {
		Notifikasjon notifikasjon = new Notifikasjon();
		notifikasjon.setStatus(FERDIGSTILT);
		DoknotifikasjonStatusTo updateStatusTo = doknotifikasjonStatusBuilder(OVERSENDT);
		assertFalse(knot004Service.statusIsNewerThanPreviousStatus(
				updateStatusTo,
				notifikasjon
		));
	}

	@Test
	public void ShouldNotUpdateStatusFeiletToOversendt() {
		Notifikasjon notifikasjon = new Notifikasjon();
		notifikasjon.setStatus(FEILET);
		DoknotifikasjonStatusTo updateStatusTo = doknotifikasjonStatusBuilder(OVERSENDT);
		assertFalse(knot004Service.statusIsNewerThanPreviousStatus(
				updateStatusTo,
				notifikasjon
		));
	}

	private DoknotifikasjonStatusTo doknotifikasjonStatusBuilder(Status status) {
		return new DoknotifikasjonStatusTo(null,null, status,null,null);
	}

}