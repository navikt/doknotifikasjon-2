package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import no.nav.doknotifikasjon.slack.SlackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot001Scheduler {

	private final Snot001Service snot001Service;
	private final LeaderElection leaderElection;
	private final SlackService slackService;

	public Snot001Scheduler(Snot001Service snot001Service,
							LeaderElection leaderElection,
							SlackService slackService) {
		this.snot001Service = snot001Service;
		this.leaderElection = leaderElection;
		this.slackService = slackService;
	}

	@Scheduled(cron = "0 30 8 * * *")
	public void scheduledJob() {
		try {
			if (leaderElection.isLeader()) {
				log.info("Snot001 pod is leader");
				snot001Service.resendNotifikasjoner();
			} else {
				log.info("Snot001 pod is not leader");
			}
		} catch (Exception exception) {
			var feilmelding = "snot001 har feilet med feilmelding=%s".formatted(exception.getMessage());
			log.error(feilmelding, exception);
			slackService.sendMelding("snot001 har feilet med exception=%s".formatted(exception.getClass().getName()));
		}
	}
}
