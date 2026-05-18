package no.nav.doknotifikasjon.exception.functional;

import lombok.Getter;

public class DoknotifikasjonDistribusjonIkkeFunnetException extends AbstractDoknotifikasjonFunctionalException {

	@Getter
	private final long notifikasjonDistribusjonId;
	public DoknotifikasjonDistribusjonIkkeFunnetException(long notifikasjonDistribusjonId, String message) {
		super(message);
		this.notifikasjonDistribusjonId = notifikasjonDistribusjonId;
	}
}
