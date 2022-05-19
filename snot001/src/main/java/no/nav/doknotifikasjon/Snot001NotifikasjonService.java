package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonDistrubisjonService;
import no.nav.doknotifikasjon.repository.NotifikasjonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class Snot001NotifikasjonService {
	private static final String SNOT001 = "SNOT001";

	private final NotifikasjonService notifikasjonService;
	private final NotifikasjonDistrubisjonService notifikasjonDistribusjonService;

	@Autowired
	public Snot001NotifikasjonService(
			NotifikasjonService notifikasjonService,
			NotifikasjonDistrubisjonService notifikasjonDistribusjonService
	) {
		this.notifikasjonService = notifikasjonService;
		this.notifikasjonDistribusjonService = notifikasjonDistribusjonService;
	}

	@Transactional
	public List<NotifikasjonDistribusjon> processNotifikasjon(Notifikasjon notifikasjon) {
		List<NotifikasjonDistribusjon> publishList = new ArrayList<>();

		Optional<NotifikasjonDistribusjon> sms = notifikasjonDistribusjonService.findFirstByNotifikasjonInAndKanal(notifikasjon, Kanal.SMS);
		Optional<NotifikasjonDistribusjon> epost = notifikasjonDistribusjonService.findFirstByNotifikasjonInAndKanal(notifikasjon, Kanal.EPOST);

		if (sms.isEmpty() && epost.isEmpty()) {
			log.error("Notifikasjon med id={} hadde ingen NotifikasjonDistribusjon", notifikasjon.getId());
			return publishList;
		}

		sms.ifPresent(nd ->
				publishList.add(persistToDBWithKanal(nd, Kanal.SMS, notifikasjon.getBestillingsId()))
		);

		epost.ifPresent(nd ->
				publishList.add(persistToDBWithKanal(nd, Kanal.EPOST, notifikasjon.getBestillingsId()))
		);

		updateNotifikasjon(notifikasjon);
		return publishList;
	}

	private void updateNotifikasjon(Notifikasjon notifikasjon) {
		int antallRenotifikasjoner = notifikasjon.getAntallRenotifikasjoner() - 1;
		LocalDate nesteRenotifikasjonDato = notifikasjon.getAntallRenotifikasjoner() > 0 ? LocalDate.now().plusDays(notifikasjon.getRenotifikasjonIntervall()) : null;

		notifikasjon.setAntallRenotifikasjoner(antallRenotifikasjoner);
		notifikasjon.setNesteRenotifikasjonDato(nesteRenotifikasjonDato);
		notifikasjon.setEndretAv(SNOT001);
		notifikasjon.setEndretDato(LocalDateTime.now());

		log.info("Snot001 oppdaterer Notifikasjon med bestillingsId={}, antallRenotifikasjoner={}, nesteRenotifikasjonDato={}",
				notifikasjon.getBestillingsId(), antallRenotifikasjoner, nesteRenotifikasjonDato);

		notifikasjonService.save(notifikasjon);
	}

	private NotifikasjonDistribusjon persistToDBWithKanal(NotifikasjonDistribusjon notifikasjonDistribusjon, Kanal kanal, String bestillingsId) {
		log.info("Snot001 oppretter ny NotifikasjonDistribusjon med kanal={} for notifikasjon med bestillingsId={}", kanal, bestillingsId);

		String text = notifikasjonDistribusjon.getTekst().startsWith("Påminnelse: ") ?
				notifikasjonDistribusjon.getTekst() : "Påminnelse: " + notifikasjonDistribusjon.getTekst();

		NotifikasjonDistribusjon newNotifikasjonDistribusjon = NotifikasjonDistribusjon.builder()
				.notifikasjon(notifikasjonDistribusjon.getNotifikasjon())
				.status(Status.OPPRETTET)
				.kanal(kanal)
				.kontaktInfo(notifikasjonDistribusjon.getKontaktInfo())
				.tittel(notifikasjonDistribusjon.getTittel())
				.tekst(text)
				.opprettetAv(SNOT001)
				.opprettetDato(LocalDateTime.now())
				.build();

		return notifikasjonDistribusjonService.save(newNotifikasjonDistribusjon);
	}
}
