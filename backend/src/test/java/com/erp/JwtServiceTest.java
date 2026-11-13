package com.erp;

import com.erp.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final UserDetails user = User.withUsername("bob").password("x").authorities("PRODUCT_READ").build();

    @Test
    void generatedTokenIsValid() {
        JwtService jwt = new JwtService("0123456789012345678901234567890123456789", 60_000);
        assertThat(jwt.validateAndGetUsername(jwt.generate(user))).isEqualTo("bob");
    }

    @Test
    void tamperedOrExpiredTokenIsRejected() {
        JwtService jwt = new JwtService("0123456789012345678901234567890123456789", 60_000);
        assertThat(jwt.validateAndGetUsername(jwt.generate(user) + "x")).isNull();
        JwtService expired = new JwtService("0123456789012345678901234567890123456789", -1000);
        assertThat(expired.validateAndGetUsername(expired.generate(user))).isNull();
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtService("short", 1000)).isInstanceOf(IllegalStateException.class);
    }
}
