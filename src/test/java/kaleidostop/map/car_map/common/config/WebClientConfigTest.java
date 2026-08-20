package kaleidostop.map.car_map.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebClientConfigTest {

    @Test
    void createsBuilderAndOsrmClientWithoutWebClientAutoConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(WebClientConfig.class);
            context.refresh();

            assertNotNull(context.getBean("webClientBuilder", WebClient.Builder.class));
            assertNotNull(context.getBean("osrmWebClient", WebClient.class));
        }
    }
}
