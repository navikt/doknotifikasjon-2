package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.doknotifikasjon.kodeverk.Status;

import java.util.List;

public record NotifikasjonInfoTo(
		@Schema(description = "ID for notifikasjonsbestillingen", example = "123456789") int id,
		@Schema(description = "ID på bestiller", example = "narmesteleder-varsel") String bestillerId,
		@Schema(description = "Status på notifikasjonsbestillingen", example = "FERDIGSTILT") Status status,
		@Schema(description = "Antall gjenstående notifikasjoner/renotifikasjoner", example = "2") int antallRenotifikasjoner,
		@Schema List<NotifikasjonDistribusjonDto> notifikasjonDistribusjoner
) {
}
