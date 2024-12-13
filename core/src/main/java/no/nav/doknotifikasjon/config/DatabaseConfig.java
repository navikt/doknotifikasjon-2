package no.nav.doknotifikasjon.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.doknotifikasjon.config.properties.DatabaseProperties;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
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

	// Roles used in the database
	//   admin: extended permissions (create, alter, delete tables etc.). Useful in e.g. database schema migrations.
	//   user: no access to create/alter/delete tables, but has normal CRUD access to insert, update, delete rows etc.
	private static final String ROLE_ADMIN = "admin";
	private static final String ROLE_USER = "user";

	@Bean
	public DataSource userDataSource(DatabaseProperties databaseProperties) {
		return dataSource(databaseProperties, ROLE_USER);
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(DatabaseProperties databaseProperties) {
		return flyway -> Flyway.configure()
				.dataSource(dataSource(databaseProperties, ROLE_ADMIN))
				.initSql(format("SET ROLE \"%s\"", formatDbRole(databaseProperties.getUrl(), ROLE_ADMIN)))
				.load()
				.migrate();
	}

	@SneakyThrows
	private HikariDataSource dataSource(DatabaseProperties databaseProperties, String role) {
		String url = databaseProperties.getUrl();
		String credentialsMountPath = databaseProperties.getCredentialsMountPath();

		log.info("Konfigurerer database for rolle={} og credentials fra Vault mount path={}", role, credentialsMountPath);

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		config.setMaximumPoolSize(4);
		config.setMinimumIdle(1);

		return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, credentialsMountPath, formatDbRole(url, role));
	}

	private String formatDbRole(String dbUrl, String role) {
		return String.join("-", getDatabaseName(dbUrl), role);
	}

	static String getDatabaseName(String dbUrl) {
		return dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
	}

}