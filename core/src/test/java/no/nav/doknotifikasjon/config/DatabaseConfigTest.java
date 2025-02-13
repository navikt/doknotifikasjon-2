package no.nav.doknotifikasjon.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static no.nav.doknotifikasjon.config.DatabaseConfig.getDatabaseName;
import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigTest {

	@ParameterizedTest
	@MethodSource
	void skalReturnereDatabasenavnFraJdbcUrl(String jdcbUrl, String forventetDatabasenavn) {
		assertThat(getDatabaseName(jdcbUrl)).isEqualTo(forventetDatabasenavn);
	}

	private static Stream<Arguments> skalReturnereDatabasenavnFraJdbcUrl() {
		return Stream.of(
				Arguments.of("jdbc:postgresql://a12abcd020.test.no:5432/doknotifikasjon-p", "doknotifikasjon-p"),
				Arguments.of("jdbc:postgresql://b12abcd022.test.local:5432/doknotifikasjon-q2", "doknotifikasjon-q2")
		);
	}

}