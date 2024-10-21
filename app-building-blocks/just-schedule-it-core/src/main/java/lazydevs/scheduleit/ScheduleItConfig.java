package lazydevs.scheduleit;

import lazydevs.mapper.utils.reflection.InitDTO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abhijeet Rai
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "schedule-it")
public class ScheduleItConfig {
    private List<String> packagesToScan = new ArrayList<>();
    private InitDTO lockProviderInit;
}
