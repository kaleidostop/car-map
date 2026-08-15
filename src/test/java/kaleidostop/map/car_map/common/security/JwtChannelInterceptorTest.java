package kaleidostop.map.car_map.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;

    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(jwtUtil, userDetailsService);
    }

    @Test
    void authenticatedConnectGetsUserPrincipal() {
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("driver@example.com")
                .password("unused")
                .authorities("ROLE_DRIVER")
                .build();
        when(jwtUtil.extractEmail("valid-token")).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtUtil.isTokenValid("valid-token", user)).thenReturn(true);

        Message<?> result = interceptor.preSend(connectMessage("Bearer valid-token"), null);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);

        assertNotNull(accessor.getUser());
        assertEquals(user.getUsername(), accessor.getUser().getName());
    }

    @Test
    void connectWithoutJwtIsDenied() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(connectMessage(null), null));
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
