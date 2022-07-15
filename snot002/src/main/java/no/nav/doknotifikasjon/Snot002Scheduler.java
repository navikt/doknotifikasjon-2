package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot002Scheduler {

	private final Snot002Service snot002Service;
	private final LeaderElection leaderElection;

	@Autowired
	public Snot002Scheduler(Snot002Service snot002Service, LeaderElection leaderElection) {
		this.snot002Service = snot002Service;
		this.leaderElection = leaderElection;
	}

	@Scheduled(cron = "0 0 14 * * *")
	public void scheduledJob() {
		try {
			if (leaderElection.isLeader()) {
				log.info("Snot002 pod is leader");
				snot002Service.oppdaterNotifikasjonStatus();
			} else {
				log.info("Snot002 pod is not leader");
			}
		} catch (Exception exception) {
			log.error("Feil i Snot002: exception={}", exception.getMessage(), exception);
		}
	}
}
