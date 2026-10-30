package com.lavinuela.shop.identity.infrastructure.config;

import com.lavinuela.shop.identity.domain.service.AccountPolicy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
class IdentityConfiguration {

    private static final String COMMON_PASSWORDS = "identity/common-passwords.txt";

    @Bean
    AccountPolicy accountPolicy() {
        return new AccountPolicy(loadCommonPasswords());
    }

    private static Set<String> loadCommonPasswords() {
        try (InputStream in = new ClassPathResource(COMMON_PASSWORDS).getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer la lista de contraseñas comunes", e);
        }
    }
}
