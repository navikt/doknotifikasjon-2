package no.nav.doknotifikasjon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Snot001Scheduler {

    private final Snot001Service snot001Service;

    public Snot001Scheduler(Snot001Service snot001Service) {
        this.snot001Service = snot001Service;
    }

    @Scheduled(cron = "30 8 * * *")
    public void scheduledJob() {
        try {
            snot001Service.resendNotifikasjoner();
        } catch (Exception exception) {
            log.error("Feil i SNOT001: ", exception);
        }
    }

}
