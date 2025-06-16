package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import no.nav.doknotifikasjon.slack.SlackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot002Scheduler {

	private final Snot002Service snot002Service;
	private final LeaderElection leaderElection;
	private final SlackService slackService;

	public Snot002Scheduler(Snot002Service snot002Service,
							LeaderElection leaderElection,
							SlackService slackService) {
		this.snot002Service = snot002Service;
		this.leaderElection = leaderElection;
		this.slackService = slackService;
	}

	@Scheduled(cron = "0 0 1 * * *")
	public void scheduledJob() {
		try {
			if (leaderElection.isLeader()) {
				log.info("Snot002 pod is leader");
				snot002Service.oppdaterNotifikasjonStatus();
			} else {
				log.info("Snot002 pod is not leader");
			}
		} catch (Exception exception) {
			var feilmelding = "snot002 har feilet med feilmelding=%s".formatted(exception.getMessage());
			log.error(feilmelding, exception);
			slackService.sendMelding("snot002 har feilet med exception=%s".formatted(exception.getClass().getName()));
		}
	}
}
