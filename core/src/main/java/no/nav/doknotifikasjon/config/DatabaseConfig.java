package no.nav.doknotifikasjon.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.config.properties.PostgresProperties;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

import static java.lang.String.format;

@Configuration
@EnableJpaRepositories(basePackages = "no.nav.doknotifikasjon")
@Profile("nais")
@Slf4j
public class DatabaseConfig {

	private static final String CLUSTER_NAME = "${nais_cluster_name}";

	// Roles used in the database
	//   admin: extended permissions (create, alter, delete tables etc.). Useful in e.g. database schema migrations.
	//   user: no access to create/alter/delete tables, but has normal CRUD access to insert, update, delete rows etc.
	private static final String ROLE_ADMIN = "admin";
	private static final String ROLE_USER = "user";

	@Bean
	public DataSource userDataSource(
			@Value(CLUSTER_NAME) final String cluster,
			PostgresProperties postgresProperties
	) {
		return dataSource(cluster, postgresProperties.getUrl(), ROLE_USER, postgresProperties.getCredentialsMountPath());
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(
			@Value(CLUSTER_NAME) final String cluster,
			PostgresProperties postgresProperties
	) {
		return flyway -> Flyway.configure()
				.dataSource(dataSource(cluster, postgresProperties.getUrl(), ROLE_ADMIN, postgresProperties.getCredentialsMountPath()))
				.initSql(format("SET ROLE \"%s\"", formatDbRole(postgresProperties.getUrl(), ROLE_ADMIN)))
				.load()
				.migrate();
	}

	@SneakyThrows
	private HikariDataSource dataSource(String cluster, String dbUrl, String role, String credentialsMountPath) {
		log.info("Konfigurerer Postgres-db i cluster={} for rolle={} og credentials fra Vault mount path={}", cluster, role, credentialsMountPath);

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setMaximumPoolSize(4);
		config.setMinimumIdle(1);

		return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, credentialsMountPath, formatDbRole(dbUrl, role));
	}

	private String formatDbRole(String dbUrl, String role) {
		return String.join("-", getDatabaseName(dbUrl), role);
	}

	private String getDatabaseName(String dbUrl) {
		return dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
	}

}