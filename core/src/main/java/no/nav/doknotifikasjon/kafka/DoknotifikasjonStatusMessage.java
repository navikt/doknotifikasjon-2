package no.nav.doknotifikasjon.kafka;

public class DoknotifikasjonStatusMessage {

	public static final String INFO_ALREADY_EXIST_IN_DATABASE = "bestillingsId allerede mottatt";
	public static final String FEILET_USER_NOT_FOUND_IN_RESERVASJONSREGISTERET = "mottaker ikke funnet i kontakt- og reservasjonsregisteret";
	public static final String FEILET_USER_RESERVED_AGAINST_DIGITAL_CONTACT = "mottaker har reservert seg mot digital kommunikasjon";
	public static final String FEILET_USER_DOES_NOT_HAVE_VALID_CONTACT_INFORMATION = "mottaker mangler gyldig kontaktinformasjon i kontakt- og reservasjonsregisteret";
	public static final String ANTALL_RENOTIFIKASJONER_REQUIRES_RENOTIFIKASJON_INTERVALL = "antallRenotifikasjoner krever at renotifikasjonIntervall er satt";
	public static final String MOBILTELEFONNUMMER_OR_EPOSTADESSE_MUST_BE_SET = "epostadresse eller mobiltelefonnummer må være satt";
	public static final String FERDIGSTILT_RENOTIFIKASJON_STANSET = "renotifikasjon er stanset";
	public static final String FEILET_FUNCTIONAL_EXCEPTION_DIGDIR_KRR_PROXY = "Funskjonell feil mot DigitalKontakinformasjon";
	public static final String FEILET_FUNCTIONAL_EXCEPTION_SIKKERHETSNIVAA = "Funksjonell feil mot sikkerhetsnivaa";
	public static final String FEILET_TECHNICAL_EXCEPTION_DATABASE = "Feilet med DataIntegrityViolationException i databasen";
	public static final String FEILET_SIKKERHETSNIVAA = "Mottaker har ikke tilgang til login med nivå 4";

	public static final String OVERSENDT_NOTIFIKASJON_PROCESSED = "Notifikasjon er behandlet og distribusjon er bestilt";
	public static final String FEILET_DATABASE_IKKE_OPPDATERT = "Oppdatering av distribusjon feilet i database";

	public static final String FERDIGSTILT_NOTIFIKASJON_SMS = "notifikasjon sendt via sms";
	public static final String FEILET_SMS_UGYLDIG_STATUS = "distribusjon til sms feilet: ugyldig status";
	public static final String FEILET_SMS_UGYLDIG_KANAL = "distribusjon til sms feilet: ugyldig kanal";

	public static final String FERDIGSTILT_NOTIFIKASJON_EPOST = "notifikasjon sendt via epost";
	public static final String FEILET_EPOST_UGYLDIG_STATUS = "distribusjon til epost feilet: ugyldig status";
	public static final String FEILET_EPOST_UGYLDIG_KANAL = "distribusjon til epost feilet: ugyldig kanal";

	public static final String FERDIGSTILT_RESENDES = "notifikasjon er FERDIGSTILT";

	private DoknotifikasjonStatusMessage(){}
}




