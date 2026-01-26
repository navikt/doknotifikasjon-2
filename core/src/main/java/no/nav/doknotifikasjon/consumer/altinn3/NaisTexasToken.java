package no.nav.doknotifikasjon.consumer.altinn3;

import com.fasterxml.jackson.annotation.JsonProperty;

record NaisTexasToken(@JsonProperty("access_token") String accessToken) {
}
