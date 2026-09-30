package lazydevs.services.basic.filter;

import lazydevs.persistence.connection.multitenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The tenant bound to {@link TenantContext} for a request is the selected tenant
 * when one is present, otherwise the user's own tenant.
 */
class BasicRequestFilterTest {

    @AfterEach
    void cleanUp() {
        RequestContext.current().clear();
        TenantContext.reset();
    }

    @Test
    void selectedTenantWinsOverOwnTenant() {
        assertEquals("selected", tenantSeenDownstream("own", "selected"));
    }

    @Test
    void ownTenantIsUsedWhenNoTenantIsSelected() {
        assertEquals("own", tenantSeenDownstream("own", null));
    }

    @Test
    void ownTenantIsUsedWhenSelectedTenantIsBlank() {
        assertEquals("own", tenantSeenDownstream("own", "  "));
    }

    @Test
    void noTenantWhenNeitherIsKnown() {
        assertNull(tenantSeenDownstream(null, null));
    }

    @Test
    void tenantIsClearedAfterTheRequest() throws Exception {
        tenantSeenDownstream("own", "selected");
        assertNull(TenantContext.getTenantId());
    }

    private static String tenantSeenDownstream(String ownTenant, String selectedTenant) {
        RequestContext.current().setTenantCode(ownTenant);
        RequestContext.current().setSelectedTenantCode(selectedTenant);
        AtomicReference<String> seen = new AtomicReference<>();
        try {
            new BasicRequestFilter().doFilter(new MockHttpServletRequest("GET", "/any"), new MockHttpServletResponse(),
                    (request, response) -> seen.set(TenantContext.getTenantId()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return seen.get();
    }
}
