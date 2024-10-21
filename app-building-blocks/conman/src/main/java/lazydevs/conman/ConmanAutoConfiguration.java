package lazydevs.conman;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import jakarta.servlet.http.HttpServlet;

/**
 * @author Abhijeet Rai
 */
@Configuration
@EnableConfigurationProperties(ConmanConfig.class)
@Import({ConmanAdminController.class, ConmanCache.class})
@DependsOn("dynaBeansGenerator")
public class ConmanAutoConfiguration {

    @Bean
    public ServletRegistrationBean<HttpServlet> conmanServlet(ConmanConfig conmanConfig, ConmanCache conmanCache) {
        ServletRegistrationBean<HttpServlet> servRegBean = new ServletRegistrationBean<>();
        servRegBean.setServlet(new ConmanServlet(conmanCache));
        servRegBean.setUrlMappings(conmanConfig.getMockServletUriMappings());
        servRegBean.setLoadOnStartup(1);
        return servRegBean;
    }

}
