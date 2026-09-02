package lazydevs.readeasy.web;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lazydevs.readeasy.config.ReadEasyConfig.MultitenancyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Extracts the tenant id from a request header into {@link TenantContext} for
 * the duration of the request, and always clears it afterwards so pooled
 * threads never carry one request's tenant into the next.
 *
 * <p>Downstream, RLS-aware components read the tenant from TenantContext -
 * e.g. {@code JdbcGeneralReader(dataSource, "app.tenant_id")} binds it to the
 * PostgreSQL session variable that row-level-security policies evaluate.</p>
 *
 * @author Abhijeet Rai
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final MultitenancyConfig config;
    private final Pattern tenantIdPattern;

    public TenantFilter(MultitenancyConfig config) {
        this.config = config;
        this.tenantIdPattern = null == config.getTenantIdPattern()
                ? null : Pattern.compile(config.getTenantIdPattern());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(config.getHeaderName());
        if (null == tenantId || tenantId.trim().isEmpty()) {
            if (config.isRequired()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required header: " + config.getHeaderName());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        tenantId = tenantId.trim();
        if (null != tenantIdPattern && !tenantIdPattern.matcher(tenantId).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid value for header: " + config.getHeaderName());
            return;
        }
        TenantContext.setTenantId(tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.reset();
        }
    }
}
