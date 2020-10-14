package no.nav.doknotifikasjon.KafkaProducer;

public class DoknotifikasjonStatusMessage {

    public final String FEILET_ALREADY_EXIST_IN_DATABASE = "bestillingsId allerede mottatt";
    public final String FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET = "mottaker ikke funnet i kontakt- og reservasjonsregisteret";
    public final String FEILET_USER_RESERVED_AGAINST_DIGITAL_KONTAKT = "mottaker har reservert seg mot digital kommunikasjon";
    public final String FEILET_USER_DP_NOT_HAVE_VALID_CONTACT_INFORMATION = "mottaker mangler gyldig kontaktinformasjon i kontakt- og reservasjonsregisteret";

    public final String OVERSENDT_NOTIFIKASJON_BEHANDLET = "Notifikasjon er behandlet og distribusjon er bestilt";

}




