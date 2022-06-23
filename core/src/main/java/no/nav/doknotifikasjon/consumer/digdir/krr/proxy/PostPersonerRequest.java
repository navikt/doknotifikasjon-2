package no.nav.doknotifikasjon.consumer.digdir.krr.proxy;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class PostPersonerRequest {

	List<String> personidenter;
}
