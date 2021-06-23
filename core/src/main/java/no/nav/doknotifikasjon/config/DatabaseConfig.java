package no.nav.doknotifikasjon.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "no.nav.doknotifikasjon")
@Profile("nais")
@Slf4j
public class DatabaseConfig {

	private static final String DOKNOTIFIKASJON_DB_URL = "${doknotifikasjon_db_url}";
	private static final String APPLICATION_NAME = "doknotifikasjon";
	private static final String CLUSTER_NAME = "${nais_cluster_name}";
	private static final String ADMIN = "admin";
	private static final String MOUNT_PATH = "${MOUNT_PATH}";

	@Bean
	public DataSource userDataSource(@Value(DOKNOTIFIKASJON_DB_URL) final String doknotifikasjonDbUrl, @Value(CLUSTER_NAME) final String cluster,
									 @Value(MOUNT_PATH) final String mountPath) {
		return dataSource("user", doknotifikasjonDbUrl, cluster, mountPath);
	}

	@SneakyThrows
	private HikariDataSource dataSource(String user, String doknotifikasjonDbUrl, String cluster, String mountPath) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(doknotifikasjonDbUrl);
		config.setMaximumPoolSize(4);
		config.setMinimumIdle(1);
		log.info("Running on cluster={}", cluster);
		log.info("Vault mountPath={}", mountPath);
		return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, mountPath, dbRole(user));
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(@Value(DOKNOTIFIKASJON_DB_URL) final String doknotifikasjonDbUrl,
														   @Value(CLUSTER_NAME) final String cluster, @Value(MOUNT_PATH) final String mountPath) {
		return flyway -> Flyway.configure()
				.dataSource(dataSource(ADMIN, doknotifikasjonDbUrl, cluster, mountPath))
				.initSql(String.format("SET ROLE \"%s\"", dbRole(ADMIN)))
				.load()
				.migrate();
	}

	private String dbRole(String role) {
		return String.join("-", APPLICATION_NAME, role);
	}
}
