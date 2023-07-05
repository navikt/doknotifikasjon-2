package no.nav.doknotifikasjon.knot002.itest.utils;

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2015._06.EndPointResult;
import no.altinn.schemas.services.serviceengine.notification._2015._06.EndPointResultList;
import no.altinn.schemas.services.serviceengine.notification._2015._06.NotificationResult;
import no.altinn.schemas.services.serviceengine.notification._2015._06.SendNotificationResultList;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;

import java.time.LocalDateTime;
import java.util.Collections;

import static no.nav.doknotifikasjon.consumer.altinn.JAXBWrapper.ns;

public final class TestUtils {

	public static final String BESTILLINGS_ID = "1234-5678-9101";
	public static final String BESTILLER_ID = "teamdokumenthandtering";
	public static final String KONTAKTINFO = "Hallohallo";
	public static final int ANTALL_RENOTIFIKASJONER = 3;
	private static final String TITTEL = "Melding";
	private static final String TEKST = "Lang tekst";
	private static final String OPPRETTET_AV = "srvdokument";
	private static final LocalDateTime SENDT_DATO = LocalDateTime.parse("2020-10-04T10:15:30.000000");
	private static final LocalDateTime OPPRETTET_DATO = LocalDateTime.parse("2020-10-01T10:15:30.000000");
	public TestUtils() {
	}

	public static Notifikasjon createNotifikasjon() {
		return Notifikasjon.builder()
				.bestillingsId(BESTILLINGS_ID)
				.bestillerId(BESTILLER_ID)
				.status(Status.OVERSENDT)
				.notifikasjonDistribusjon(Collections.emptySet())
				.opprettetDato(OPPRETTET_DATO)
				.antallRenotifikasjoner(ANTALL_RENOTIFIKASJONER)
				.build();
	}

	public static NotifikasjonDistribusjon createNotifikasjonDistribusjonWithNotifikasjonIdAndStatus(
			Notifikasjon notifikasjon,
			Status status,
			Kanal kanal
	) {
		return NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjon)
				.status(status)
				.kanal(kanal)
				.kontaktInfo(KONTAKTINFO)
				.tittel(TITTEL)
				.tekst(TEKST)
				.sendtDato(SENDT_DATO)
				.opprettetAv(OPPRETTET_AV)
				.opprettetDato(OPPRETTET_DATO)
				.build();
	}

	public static SendNotificationResultList generateAltinnResponse(TransportType transportType, String kontaktinfo) {
		var endPointResult = new EndPointResult();
		endPointResult.setName(ns("Name", "Knot To"));
		endPointResult.setReceiverAddress(ns("ReceiverAddress", kontaktinfo));
		endPointResult.setTransportType(transportType);

		var endPointResultList = new EndPointResultList();
		endPointResultList.getEndPointResult().add(endPointResult);

		var notificationResult =
				new NotificationResult();
		notificationResult.setEndPoints(
				ns(
						"EndPointResultList",
						EndPointResultList.class,
						endPointResultList
				)
		);
		notificationResult.setNotificationType(ns("NotificationType", "TokenTextOnly"));

		SendNotificationResultList sendNotificationResultList = new SendNotificationResultList();
		sendNotificationResultList.getNotificationResult().add(notificationResult);
		return sendNotificationResultList;
	}
}