package kaleidostop.map.car_map.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient osrmWebClient(WebClient.Builder builder,
                                   @Value("${osrm.base-url:https://router.project-osrm.org}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
