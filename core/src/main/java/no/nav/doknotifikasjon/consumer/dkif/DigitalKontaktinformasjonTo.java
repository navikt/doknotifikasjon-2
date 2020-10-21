package no.nav.doknotifikasjon.consumer.dkif;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalKontaktinformasjonTo {

    private Map<String, Melding> feil;
    private Map<String, DigitalKontaktinfo> kontaktinfo;

    @Data
    private static class Melding {
        private String melding;
    }

    @Data
    @Builder
    public static class DigitalKontaktinfo {
        private String epostadresse;
        private boolean kanVarsles;
        private String mobiltelefonnummer;
        private boolean reservert;
    }

}