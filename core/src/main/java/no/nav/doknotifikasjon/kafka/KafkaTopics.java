package no.nav.doknotifikasjon.kafka;

public class KafkaTopics {
	public static final String KAFKA_TOPIC_DOK_NOTIFKASJON = "teamdokumenthandtering.privat-dok-notifikasjon";
	public static final String PRIVAT_DOK_NOTIFIKASJON_MED_KONTAKT_INFO = "teamdokumenthandtering.privat-dok-notifikasjon-med-kontakt-info";
	public static final String KAFKA_TOPIC_DOK_NOTIFKASJON_STATUS = "teamdokumenthandtering.privat-dok-notifikasjon-status";
	public static final String KAFKA_TOPIC_DOK_NOTIFKASJON_SMS = "teamdokumenthandtering.privat-dok-notifikasjon-sms";
	public static final String KAFKA_TOPIC_DOK_NOTIFKASJON_EPOST = "teamdokumenthandtering.privat-dok-notifikasjon-epost";
	public static final String KAFKA_TOPIC_DOK_NOTIFIKASJON_STOPP = "teamdokumenthandtering.privat-dok-notifikasjon-stopp";

	private KafkaTopics(){}
}