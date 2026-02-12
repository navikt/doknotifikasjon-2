package no.nav.doknotifikasjon;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.doknotifikasjon.config.LokalCacheConfig.ALTINN_TOKEN_CACHE;
import static no.nav.doknotifikasjon.config.LokalCacheConfig.AZURE_TOKEN_CACHE;


public class LocalTestCacheConfig {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(List.of(
			new CaffeineCache(AZURE_TOKEN_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(0, TimeUnit.MINUTES)
				.maximumSize(0)
				.build()),
			new CaffeineCache(ALTINN_TOKEN_CACHE, Caffeine.newBuilder()
				.expireAfterWrite(0, TimeUnit.MINUTES)
				.maximumSize(0)
				.build())
		));
		return manager;
	}
}
