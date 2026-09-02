package lazydevs.readeasy.web;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lazydevs.readeasy.config.ReadEasyConfig.MultitenancyConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantFilterTest {

    private final List<String> errors = new ArrayList<>();
    private final AtomicReference<String> tenantSeenByChain = new AtomicReference<>();

    private HttpServletRequest request(String headerName, String headerValue) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName())) {
                        return headerName.equalsIgnoreCase((String) args[0]) ? headerValue : null;
                    }
                    throw new UnsupportedOperationException("HttpServletRequest." + method.getName());
                });
    }

    private HttpServletResponse response() {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
                    if ("sendError".equals(method.getName())) {
                        errors.add(args[0] + ":" + args[1]);
                        return null;
                    }
                    throw new UnsupportedOperationException("HttpServletResponse." + method.getName());
                });
    }

    private FilterChain chain() {
        return (request, response) -> tenantSeenByChain.set(TenantContext.getTenantId());
    }

    private MultitenancyConfig config() {
        MultitenancyConfig config = new MultitenancyConfig();
        config.setEnabled(true);
        return config;
    }

    @AfterEach
    void cleanup() {
        TenantContext.reset();
    }

    @Test
    void setsTenantForChainAndClearsAfterwards() throws Exception {
        new TenantFilter(config()).doFilterInternal(
                request("X-Tenant-Id", "tenant-42"), response(), chain());
        assertEquals("tenant-42", tenantSeenByChain.get());
        assertNull(TenantContext.getTenantId(), "tenant must be cleared after the request");
        assertTrue(errors.isEmpty());
    }

    @Test
    void clearsTenantEvenWhenChainThrows() {
        try {
            new TenantFilter(config()).doFilterInternal(
                    request("X-Tenant-Id", "t1"), response(),
                    (request, response) -> { throw new RuntimeException("boom"); });
        } catch (Exception expected) {
            // ignore
        }
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void missingHeaderIsRejectedWhenRequired() throws Exception {
        new TenantFilter(config()).doFilterInternal(
                request("X-Tenant-Id", null), response(), chain());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("400:"));
        assertNull(tenantSeenByChain.get(), "chain must not run");
    }

    @Test
    void missingHeaderPassesThroughWhenOptional() throws Exception {
        MultitenancyConfig config = config();
        config.setRequired(false);
        AtomicReference<Boolean> chainRan = new AtomicReference<>(false);
        new TenantFilter(config).doFilterInternal(
                request("X-Tenant-Id", null), response(),
                (request, response) -> chainRan.set(true));
        assertTrue(chainRan.get());
        assertTrue(errors.isEmpty());
    }

    @Test
    void customHeaderNameIsHonored() throws Exception {
        MultitenancyConfig config = config();
        config.setHeaderName("X-Org-Id");
        new TenantFilter(config).doFilterInternal(
                request("X-Org-Id", "org-7"), response(), chain());
        assertEquals("org-7", tenantSeenByChain.get());
    }

    @Test
    void patternMismatchIsRejected() throws Exception {
        MultitenancyConfig config = config();
        config.setTenantIdPattern("[0-9a-f-]{36}");
        new TenantFilter(config).doFilterInternal(
                request("X-Tenant-Id", "not-a-uuid'; drop table employee"), response(), chain());
        assertEquals(1, errors.size());
        assertNull(tenantSeenByChain.get());
    }

    @Test
    void patternMatchPasses() throws Exception {
        MultitenancyConfig config = config();
        config.setTenantIdPattern("[0-9a-f-]{36}");
        String uuid = "0b862a1f-3d5e-4c6a-9b21-8d3f5a7c9e01";
        new TenantFilter(config).doFilterInternal(
                request("X-Tenant-Id", uuid), response(), chain());
        assertEquals(uuid, tenantSeenByChain.get());
    }

    @Test
    void headerValueIsTrimmed() throws Exception {
        new TenantFilter(config()).doFilterInternal(
                request("X-Tenant-Id", "  t1  "), response(), chain());
        assertEquals("t1", tenantSeenByChain.get());
    }
}
