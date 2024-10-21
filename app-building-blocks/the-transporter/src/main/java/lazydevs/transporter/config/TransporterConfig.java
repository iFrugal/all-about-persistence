package lazydevs.transporter.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Configuration
@ConfigurationProperties(prefix = "transporter")
@Getter @Setter @ToString
public class TransporterConfig {
    private Map<String, String> pipelines = new HashMap<>();

}


