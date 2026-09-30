package lazydevs.readeasy.it;

import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots a real web application with read-easy on the classpath and calls the
 * /read endpoints over HTTP. Subclasses supply the host application: one relying
 * purely on auto-configuration, one that also imports DynaBeansAutoConfiguration
 * by hand (the workaround hosts used while auto-configuration was not registered).
 *
 * <p>Uses the {@code readeasy-it} profile: embedded H2, a JdbcGeneralReader, one
 * query ({@code items.all}) and multitenancy with the default {@code X-Tenant-Id}
 * header on {@code /read/*}.</p>
 */
public abstract class ReadEasyContextTest {

    private static final String LIST_URI = "/read/list?queryId=items.all";

    @Autowired private ApplicationContext applicationContext;
    @Value("${local.server.port}") private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void dynaBeansAreAutoConfiguredExactlyOnce() {
        assertEquals(1, applicationContext.getBeansOfType(DynaBeansAutoConfiguration.class).size());
        assertTrue(applicationContext.containsBean("dynaBeansGenerator"));
    }

    @Test
    void listWithoutTenantHeaderIsRejected() throws Exception {
        HttpResponse<String> response = postList(null);
        assertEquals(400, response.statusCode());
    }

    @Test
    void listWithTenantHeaderReturnsRowsAsJson() throws Exception {
        HttpResponse<String> response = postList("tenant-a");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
                "Content-Type: " + response.headers().firstValue("Content-Type").orElse(null));
        String body = response.body().replace(" ", "").toLowerCase();
        assertTrue(body.startsWith("[") && body.contains("\"name\":\"alpha\"") && body.contains("\"name\":\"beta\""),
                response.body());
    }

    private HttpResponse<String> postList(String tenantId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + LIST_URI))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (null != tenantId) {
            request.header("X-Tenant-Id", tenantId);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
