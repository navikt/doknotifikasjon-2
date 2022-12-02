package no.nav.doknotifikasjon;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

import java.time.LocalDateTime;

public record NotifikasjonDistribusjonDto(
		@Schema(description = "Unik ID for notifikasjon (sms/epost)", example = "227221337") int id,
		@Schema(description = "Status på notifikasjon (sms/epost)", example = "FERDIGSTILT") Status status,
		@Schema(description = "Kanal notifikasjonen har blitt sendt i (sms eller epost)", example = "SMS") Kanal kanal,
		@Schema(description = "Epostadresse/telefonnummer notifikasjonen er sendt til", example = "98765432") String kontaktInfo,
		@Schema(description = "Tittel - kun satt hvis kanal=epost", example = " ") String tittel,
		@Schema(description = "Innhold i sms/epost-varsel", example = "Du har fått et brev fra NAV.") String tekst,
		@Schema(description = "Tidspunkt når sms/epost ble sendt", example = "2007-12-03T10:15:30") LocalDateTime sendtDato
) {
}
