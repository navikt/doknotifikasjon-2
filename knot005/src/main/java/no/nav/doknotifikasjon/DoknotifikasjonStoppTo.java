package no.nav.doknotifikasjon;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class DoknotifikasjonStoppTo {
    String bestillingsId;
    String bestillerId;
}
