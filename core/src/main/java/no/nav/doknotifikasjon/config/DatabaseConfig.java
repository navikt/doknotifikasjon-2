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

import static java.lang.String.format;

@Configuration
@EnableJpaRepositories(basePackages = "no.nav.doknotifikasjon")
@Profile("nais")
@Slf4j
public class DatabaseConfig {

	private static final String DB_URL = "${doknotifikasjon_db_url}";
	private static final String DB_NAME = "doknotifikasjon";
	private static final String CLUSTER_NAME = "${nais_cluster_name}";
	private static final String MOUNT_PATH = "${MOUNT_PATH}";

	// Roles used in the database
	//   admin: extended permissions (create, alter, delete tables etc.). Useful in e.g. database schema migrations.
	//   user: no access to create/alter/delete tables, but has normal CRUD access to insert, update, delete rows etc.
	private static final String ROLE_ADMIN = "admin";
	private static final String ROLE_USER = "user";

	@Bean
	public DataSource userDataSource(
			@Value(DB_URL) final String doknotifikasjonDbUrl,
			@Value(CLUSTER_NAME) final String cluster,
			@Value(MOUNT_PATH) final String mountPath
	) {
		return dataSource(ROLE_USER, doknotifikasjonDbUrl, cluster, mountPath);
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(
			@Value(DB_URL) final String doknotifikasjonDbUrl,
			@Value(CLUSTER_NAME) final String cluster,
			@Value(MOUNT_PATH) final String mountPath
	) {
		return flyway -> Flyway.configure()
				.dataSource(dataSource(ROLE_ADMIN, doknotifikasjonDbUrl, cluster, mountPath))
				.initSql(format("SET ROLE \"%s\"", formatDbRole(ROLE_ADMIN)))
				.load()
				.migrate();
	}

	@SneakyThrows
	private HikariDataSource dataSource(String role, String doknotifikasjonDbUrl, String cluster, String mountPath) {
		log.info("Running on cluster={}", cluster);
		log.info("Vault mountPath={}", mountPath);

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(doknotifikasjonDbUrl);
		config.setMaximumPoolSize(4);
		config.setMinimumIdle(1);

		return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, mountPath, formatDbRole(role));
	}

	private String formatDbRole(String role) {
		return String.join("-", DB_NAME, role);
	}

}