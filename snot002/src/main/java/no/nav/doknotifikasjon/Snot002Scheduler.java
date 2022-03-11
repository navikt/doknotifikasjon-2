package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot002Scheduler {

	private final Snot002Service service;
	private final LeaderElection leaderElection;

	@Autowired
	public Snot002Scheduler(Snot002Service service, LeaderElection leaderElection) {
		this.service = service;
		this.leaderElection = leaderElection;
	}

	//TODO Legge Scheduled til når doknotifikasjon blir sannert
	//@Scheduled(cron = "0 0 1 * * *")
	public void scheduledJob() {
		try {
			if (leaderElection.isLeader()) {
				service.resendNotifikasjoner();
			}
		} catch (Exception exception) {
			log.error("Feil i SNOT002: ", exception);
		}
	}
}
