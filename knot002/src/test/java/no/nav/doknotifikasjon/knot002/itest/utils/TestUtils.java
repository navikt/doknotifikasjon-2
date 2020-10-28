package no.nav.doknotifikasjon.knot002.itest.utils;

import no.altinn.springsoap.client.gen.EndPointResult;
import no.altinn.springsoap.client.gen.EndPointResultList;
import no.altinn.springsoap.client.gen.NotificationResult;
import no.altinn.springsoap.client.gen.SendNotificationResultList;
import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3Response;
import no.altinn.springsoap.client.gen.TransportType;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.altinn.springsoap.client.gen.ObjectFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

public final class TestUtils {

	public TestUtils() {
	}

	public static final String NOTIFIKASJONDISTRIBUSJONID = "2147483647";
	public static final String BESTILLINGS_ID = "1234-5678-9101";
	public static final String BESTILLER_ID = "teamdokumenthandtering";
	public static final String BESTILLER_ID_2 = "teamsaf";
	public static final String MELDING = "Heiheihei";
	public static final String KONTAKTINFO = "Hallohallo";
	private static final String TITTEL = "Melding";
	private static final String TEKST = "Lang tekst";
	private static final String OPPRETTET_AV = "srvdokument";
	public static final int ANTALL_RENOTIFIKASJONER = 3;
	public static final Long DISTRIBUSJON_ID = 987654321L;
	private static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	public static final Status STATUS_OPPRETTET = Status.OPPRETTET;

	private static final ObjectFactory objectfactory = new ObjectFactory();

	public static Notifikasjon createNotifikasjon() {
		return Notifikasjon.builder()
				.bestillingsId(BESTILLINGS_ID)
				.bestillerId(BESTILLER_ID)
				.status(Status.FEILET)
				.notifikasjonDistribusjon(Collections.emptySet())
				.opprettetDato(OPPRETTET_DATO)
				.antallRenotifikasjoner(ANTALL_RENOTIFIKASJONER)
				.build();
	}

	public static Notifikasjon createNotifikasjonWithDistribusjon(
			Status status
	) {
		Notifikasjon notifikasjon = createNotifikasjon();
		NotifikasjonDistribusjon notifikasjonDistribusjon = createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(
				notifikasjon,
				status
		);

		notifikasjon.setNotifikasjonDistribusjon(Set.of(notifikasjonDistribusjon));

		return notifikasjon;
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(
			Notifikasjon notifikasjon,
			Status status
	) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(status)
				.kanal(Kanal.SMS)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.build();
	}

	public static SendStandaloneNotificationBasicV3Response generateAltinnResponse(
			TransportType transportType,
			String address
	){
		SendStandaloneNotificationBasicV3Response sendStandaloneNotificationBasicV3Response =
				objectfactory.createSendStandaloneNotificationBasicV3Response();

		SendNotificationResultList sendNotificationResultList = objectfactory.createSendNotificationResultList();

		NotificationResult notificationResult = objectfactory.createNotificationResult();

		EndPointResultList endPointResultList = objectfactory.createEndPointResultList();

		EndPointResult endPointResult = objectfactory.createEndPointResult();

		endPointResult.setTransportType(transportType);
		endPointResult.setReceiverAddress(objectfactory.createEndPointResultReceiverAddress(address));

		endPointResultList.getEndPointResult().add(endPointResult);

		notificationResult.setEndPoints(objectfactory.createEndPointResultList(endPointResultList));

		sendNotificationResultList.getNotificationResult().add(notificationResult);

		sendStandaloneNotificationBasicV3Response.setSendStandaloneNotificationBasicV3Result(
				objectfactory.createSendNotificationResultList(sendNotificationResultList)
		);
		return sendStandaloneNotificationBasicV3Response;
	}
}