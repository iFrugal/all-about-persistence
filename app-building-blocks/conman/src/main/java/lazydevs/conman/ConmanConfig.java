package lazydevs.conman;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Abhijeet Rai
 */
@ConfigurationProperties(prefix = "conman")
@Getter @Setter @ToString
public class ConmanConfig {
    private List<String> mockServletUriMappings = Arrays.asList("/mock/*");
    private List<String> mockMappingFiles = new ArrayList<>(Arrays.asList("classpath:conman.yml"));
    private String readInstruction = "{\"find\" : {\"filter\" : {} }}"; //mongodb specific instruction defaulted

}
