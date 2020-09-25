package no.nav.doknotifikasjon.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ContainerAwareErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class KafkaErrorHandler implements ContainerAwareErrorHandler {

    private final static int DURATION = 20;
    private final MeterRegistry meterRegistry;

    public KafkaErrorHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handle(Exception e, List<ConsumerRecord<?, ?>> list,
                       Consumer<?, ?> consumer,
                       MessageListenerContainer messageListenerContainer) {

        Throwable throwable = e.getCause() == null ? e : e.getCause();
        String exceptionName = throwable.getClass().getSimpleName();

        log.warn("KafkaContainer feilet med feilmelding={}", throwable.getMessage(), throwable);
        meterRegistry.counter("dok_exception", "type", "technical", "exception_name", exceptionName).increment();

        if(list.size() == 1) {
            ConsumerRecord<?, ?> record = list.get(0);
            log.warn("Failed to commit offset {} on partition {}", record.offset(), record.partition());
        }

        log.warn("Thread {} sleeping {} seconds to try again", Thread.currentThread().getId(), DURATION);
        try {
            Thread.sleep(Duration.ofSeconds(DURATION).toMillis());
        } catch (InterruptedException ie) {
        }
    }
}
