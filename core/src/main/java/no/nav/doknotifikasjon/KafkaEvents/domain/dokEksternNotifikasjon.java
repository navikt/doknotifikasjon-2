package no.nav.doknotifikasjon.KafkaEvents.domain;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class dokEksternNotifikasjon {
    String bestillingsId;
    String bestillerId;
    String fodselsnummer;
    Integer antallRenotifikasjoner;
    Integer renotifikasjonIntervall;
    String tittel;
    String tekst;
    String prefererteKanaler;
}
