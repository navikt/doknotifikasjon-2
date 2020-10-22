package no.nav.doknotifikasjon.KafkaProducer;

public class DoknotifikasjonStatusMessage {

    public static final String FEILET_ALREADY_EXIST_IN_DATABASE = "bestillingsId allerede mottatt";
    public static final String FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET = "mottaker ikke funnet i kontakt- og reservasjonsregisteret";
    public static final String FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT = "mottaker har reservert seg mot digital kommunikasjon";
    public static final String FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION = "mottaker mangler gyldig kontaktinformasjon i kontakt- og reservasjonsregisteret";
    public static final String FEILET_CANT_CONNECT_TO_DKIF = "Får ikke kontakt med DigitalKontaktinformasjon";
    public static final String FEILET_FIELD_RENOTIFIKASJON_INTERVALL_REQUIRES_ANTALL_RENOTIFIKASJONER = "RenotifikasjonIntervall krever at antallRenotifikasjoner er satt";

    public static final String OVERSENDT_NOTIFIKASJON_PROCESSED = "Notifikasjon er behandlet og distribusjon er bestilt";

}




