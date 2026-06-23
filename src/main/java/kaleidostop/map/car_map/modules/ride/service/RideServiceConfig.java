package kaleidostop.map.car_map.modules.ride.service;

import kaleidostop.map.car_map.modules.ride.service.rules.MaxDetourMetersRule;
import kaleidostop.map.car_map.modules.ride.service.rules.MaxDetourSecondsRule;
import kaleidostop.map.car_map.modules.ride.service.rules.RideJoinRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RideServiceConfig {
    @Bean
    public RideJoinRule maxDetourMetersRule() {
        return new MaxDetourMetersRule();
    }

    @Bean
    public RideJoinRule maxDetourSecondsRule() {
        return new MaxDetourSecondsRule();
    }
}
