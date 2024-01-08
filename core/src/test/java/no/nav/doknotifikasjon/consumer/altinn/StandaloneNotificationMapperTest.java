package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.StandaloneNotification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType.EMAIL;
import static no.nav.doknotifikasjon.kodeverk.Kanal.EPOST;
import static org.assertj.core.api.Assertions.assertThat;

class StandaloneNotificationMapperTest {

	private static final String FOEDSELSNUMMER = "01010156798";
	private static final String EPOST_TITTEL = "You've got mail!";
	private static final String EPOST_TEKST = "Dette er innholdet i en epost.";
	private static final String EPOST_ADRESSE = "epost@mottaker.no";
	private static final String TELEFONNUMMER = "+4712349876";
	private static final String SMS_TEKST = "Viktig melding fra NAV";

	@ParameterizedTest
	@MethodSource
	public void shouldMapStandaloneNotification(Kanal kanal, String kontaktinformasjon, String fnr, String tekst, String tittel, TransportType transportType) {
		List<StandaloneNotification> standaloneNotifications = StandaloneNotificationMapper.map(kanal, kontaktinformasjon, fnr, tekst, tittel).getStandaloneNotification();

		standaloneNotifications.forEach(notification -> {
			assertThat(notification.getReporteeNumber().getValue()).isEqualTo(FOEDSELSNUMMER);
			assertThat(notification.getLanguageID().intValue()).isEqualTo(1044);
			notification.getReceiverEndPoints().getValue().getReceiverEndPoint().forEach(receiverEndPoint ->
					assertThat(receiverEndPoint.getTransportType().getValue()).isEqualTo(transportType));
			assertEpostAndSMSToken(notification.getTextTokens().getValue(), tekst, tittel);
		});
	}

	private void assertEpostAndSMSToken(TextTokenSubstitutionBEList textTokenSubstitution, String tekst, String tittel) {
		textTokenSubstitution.getTextToken()
				.forEach(textToken -> assertThat(textToken.getTokenValue().getValue()).containsAnyOf(tekst, tittel));
	}

	private static Stream<Arguments> shouldMapStandaloneNotification() {
		return Stream.of(
				Arguments.of(EPOST, EPOST_ADRESSE, FOEDSELSNUMMER, EPOST_TEKST, EPOST_TITTEL, EMAIL),
				Arguments.of(Kanal.SMS, TELEFONNUMMER, FOEDSELSNUMMER, SMS_TEKST, "", TransportType.SMS)
		);
	}
}