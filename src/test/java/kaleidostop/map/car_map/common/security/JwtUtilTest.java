package kaleidostop.map.car_map.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {
    private static final String SECRET = "unit-test-secret-key-that-is-at-least-thirty-two-bytes-long";
    private static final UserDetails USER = org.springframework.security.core.userdetails.User
            .withUsername("passenger@example.com")
            .password("unused")
            .authorities("ROLE_USER")
            .build();

    @Test
    void generatedTokenContainsSubjectAndIsValidForItsOwner() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

        String token = jwtUtil.generateToken(USER);

        assertEquals(USER.getUsername(), jwtUtil.extractEmail(token));
        assertTrue(jwtUtil.isTokenValid(token, USER));
    }

    @Test
    void tokenIsRejectedForAnotherUser() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        UserDetails anotherUser = org.springframework.security.core.userdetails.User
                .withUsername("other@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();

        assertFalse(jwtUtil.isTokenValid(jwtUtil.generateToken(USER), anotherUser));
    }

    @Test
    void expiredAndTamperedTokensAreRejected() throws InterruptedException {
        JwtUtil shortLivedJwt = new JwtUtil(SECRET, 1);
        String expiredToken = shortLivedJwt.generateToken(USER);
        Thread.sleep(5);

        JwtUtil anotherSigner = new JwtUtil(
                "another-unit-test-secret-key-that-is-at-least-thirty-two-bytes", 60_000);

        assertFalse(shortLivedJwt.isTokenValid(expiredToken, USER));
        assertFalse(anotherSigner.isTokenValid(new JwtUtil(SECRET, 60_000).generateToken(USER), USER));
    }
}
