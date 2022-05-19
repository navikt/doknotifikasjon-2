package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot001Scheduler {

	private final Snot001Service snot001Service;
	private final LeaderElection leaderElection;

	@Autowired
	public Snot001Scheduler(Snot001Service snot001Service, LeaderElection leaderElection) {
		this.snot001Service = snot001Service;
		this.leaderElection = leaderElection;
	}

	@Scheduled(cron = "0 */5 * * * *")
	public void scheduledJob() {
		try {
			if (leaderElection.isLeader()) {
				log.info("Snot001 pod is leader");
				snot001Service.resendNotifikasjoner();
			} else {
				log.info("Snot001 pod is not leader");
			}
		} catch (Exception exception) {
			log.error("Feil i Snot001: exception={}", exception.getMessage(), exception);
		}
	}
}
