package no.nav.doknotifikasjon.kodeverk;

public enum Status {
	INFO(0),
	OPPRETTET(1),
	OVERSENDT(2),
	FERDIGSTILT(3),
	FEILET(4);

	int priority;

	Status(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}
