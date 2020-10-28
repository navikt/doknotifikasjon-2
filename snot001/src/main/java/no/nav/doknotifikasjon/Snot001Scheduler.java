package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.leaderelection.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

@Slf4j
@Component
public class Snot001Scheduler {

    private final Snot001Service snot001Service;
    private final LeaderElection leaderElection;

    @Inject
    public Snot001Scheduler(Snot001Service snot001Service, LeaderElection leaderElection) {
        this.snot001Service = snot001Service;
        this.leaderElection = leaderElection;
    }

    @Transactional
    @Scheduled(cron = "0 30 8 * * *")
    public void scheduledJob() {
        try {
            if (leaderElection.isLeader()) {
                snot001Service.resendNotifikasjoner();
            }
        } catch (Exception exception) {
            log.error("Feil i SNOT001: ", exception);
        }
    }
}
