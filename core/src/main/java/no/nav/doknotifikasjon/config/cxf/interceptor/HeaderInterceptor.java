package no.nav.doknotifikasjon.config.cxf.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;
import static org.apache.cxf.phase.Phase.PRE_PROTOCOL_ENDING;

/**
 * Interceptor for å legge til attributten <i>Connection</i> med verdi <i>Keep-Alive</i>
 * i header på utgående webservice melding.
 */
@Slf4j
public class HeaderInterceptor extends AbstractPhaseInterceptor<Message> {

    public HeaderInterceptor() {
        super(PRE_PROTOCOL_ENDING);
        getAfter().add(SAAJOutInterceptor.SAAJOutEndingInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, List> headers = (Map<String, List>) message.get(PROTOCOL_HEADERS);
        headers.put("Connection", singletonList("Keep-Alive"));
    }

}
