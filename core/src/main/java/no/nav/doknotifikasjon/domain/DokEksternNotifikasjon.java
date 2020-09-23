package no.nav.doknotifikasjon.domain;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DokEksternNotifikasjon {
    String bestillingsId;
    String bestillerId;
    String fodselsnummer;
    Integer antallRenotifikasjoner;
    Integer renotifikasjonIntervall;
    String tittel;
    String tekst;
    String prefererteKanaler;
}
