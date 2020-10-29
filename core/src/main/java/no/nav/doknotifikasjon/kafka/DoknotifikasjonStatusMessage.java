package no.nav.doknotifikasjon.kafka;

public class DoknotifikasjonStatusMessage {

    public static final String INFO_ALREADY_EXIST_IN_DATABASE = "bestillingsId allerede mottatt";
    public static final String FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET = "mottaker ikke funnet i kontakt- og reservasjonsregisteret";
    public static final String FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT = "mottaker har reservert seg mot digital kommunikasjon";
    public static final String FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION = "mottaker mangler gyldig kontaktinformasjon i kontakt- og reservasjonsregisteret";
    public static final String INFO_CANT_CONNECT_TO_DKIF = "Får ikke kontakt med DigitalKontaktinformasjon";
    public static final String FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER = "RenotifikasjonIntervall krever at antallRenotifikasjoner er satt";
    public static final String FERDIGSTILT_RENOTIFIKASJON_STANSET = "renotifikasjon er stanset";

    public static final String OVERSENDT_NOTIFIKASJON_PROCESSED = "Notifikasjon er behandlet og distribusjon er bestilt";
    public static final String FERDIGSTILLT = "notifikasjon sendt via sms";
    public static final String UGYLDIG_STATUS = "distribusjon til sms feilet: ugyldig status";
    public static final String UGYLDIG_KANAL = "distribusjon til sms feilet: ugyldig kanal";
    public static final String IKKE_OPPDATERT = "Oppdatering av distrubusjon feilet i database";

}




