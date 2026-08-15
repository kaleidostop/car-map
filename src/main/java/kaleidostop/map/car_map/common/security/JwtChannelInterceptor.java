package kaleidostop.map.car_map.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT != accessor.getCommand()) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("JWT is required for WebSocket connections");
        }

        try {
            String token = authorization.substring(BEARER_PREFIX.length());
            String email = jwtUtil.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtUtil.isTokenValid(token, userDetails)) {
                throw new AccessDeniedException("Invalid JWT");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Invalid JWT", ex);
        }
    }
}
