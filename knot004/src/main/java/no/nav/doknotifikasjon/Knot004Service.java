package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.model.Notifikasjon;
import no.nav.doknotifikasjon.model.NotifikasjonDistribusjon;
import no.nav.doknotifikasjon.repository.NotifikasjonRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class Knot004Service {

	private final NotifikasjonRepository notifikasjonRepository;
	private final Knot004Validator knot004Validator;

	public Knot004Service(NotifikasjonRepository notifikasjonRepository, Knot004Validator knot004Validator) {
		this.notifikasjonRepository = notifikasjonRepository;
		this.knot004Validator = knot004Validator;
	}

	public void shouldUpdateStatus(DoknotifikasjonStatusTo doknotifikasjonStatusTo){

		knot004Validator.shouldValidateInput(doknotifikasjonStatusTo);

		//2) Valider hendelse: Hent bestilling fra databasen		todo: Hva mer skal valideres?
		Notifikasjon notifikasjon = notifikasjonRepository.getByBestillingId(doknotifikasjonStatusTo.getBestillingId());

		//3) Oppdater status
		if(doknotifikasjonStatusTo.getDistribusjonId() != null){
			Set<NotifikasjonDistribusjon> notifikasjonDistribusjonSet = notifikasjon.getNotifikasjonDistribusjon();

			for(NotifikasjonDistribusjon notifikasjonDistribusjon : notifikasjonDistribusjonSet){
				if(doknotifikasjonStatusTo.getStatus().equals(notifikasjonDistribusjon.getStatus())){

				}
			}
		}
	}

//	private boolean
}
