package no.nav.doknotifikasjon.consumer.altinn;

import no.altinn.springsoap.client.gen.SendStandaloneNotificationBasicV3;
import org.mockito.ArgumentMatcher;

public class RequestMatcher implements ArgumentMatcher<SendStandaloneNotificationBasicV3> {

    private final SendStandaloneNotificationBasicV3 objectToCompareWith;

    RequestMatcher(SendStandaloneNotificationBasicV3 objectToCompareWith) {
        this.objectToCompareWith = objectToCompareWith;
    }

    @Override
    public boolean matches(SendStandaloneNotificationBasicV3 request) {
        if(!request.getSystemUserName().equals(objectToCompareWith.getSystemUserName())){
            return false;
        }
        if(!request.getSystemPassword().equals(objectToCompareWith.getSystemPassword())){
            return false;
        }
        return true;
    }
}
