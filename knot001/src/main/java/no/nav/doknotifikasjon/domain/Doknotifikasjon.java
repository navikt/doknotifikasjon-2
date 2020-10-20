package no.nav.doknotifikasjon.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Doknotifikasjon {
    public String bestillingsId;
    public String bestillerId;
    public String fodselsnummer;
    public Integer antallRenotifikasjoner;
    public Integer renotifikasjonIntervall;
    public String tittel;
    public String tekst;
}