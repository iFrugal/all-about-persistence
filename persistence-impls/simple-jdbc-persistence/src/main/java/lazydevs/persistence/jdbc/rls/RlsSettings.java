package lazydevs.persistence.jdbc.rls;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lombok.Getter;
import lombok.ToString;

import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Settings for row-level-security tenant binding. Immutable.
 *
 * <p>The setting name must be a custom PostgreSQL GUC of the form
 * {@code prefix.name} (e.g. {@code app.tenant_id}) matching what the table's
 * RLS policy reads via {@code current_setting('app.tenant_id', true)}.</p>
 *
 * @author Abhijeet Rai
 */
@Getter @ToString
public class RlsSettings {

    public static final String DEFAULT_SETTING_NAME = "app.tenant_id";

    /** Custom GUCs require a dot-separated two-part name. */
    private static final Pattern SETTING_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z_][A-Za-z0-9_]*");

    /** The PostgreSQL session variable the RLS policy reads. */
    private final String settingName;

    /**
     * When true (the default), acquiring a connection with no tenant in scope
     * throws instead of returning an unbound connection. The database policy
     * fails closed either way; this makes the mistake loud instead of silent.
     */
    private final boolean failWhenTenantMissing;

    /** Where the current tenant comes from. Defaults to {@link TenantContext#getTenantId()}. */
    @ToString.Exclude
    private final Supplier<String> tenantSupplier;

    public RlsSettings(String settingName, boolean failWhenTenantMissing, Supplier<String> tenantSupplier) {
        if (null == settingName || !SETTING_NAME_PATTERN.matcher(settingName).matches()) {
            throw new IllegalArgumentException("RLS settingName must be a two-part custom GUC name like 'app.tenant_id', but was: " + settingName);
        }
        if (null == tenantSupplier) {
            throw new IllegalArgumentException("tenantSupplier is required");
        }
        this.settingName = settingName;
        this.failWhenTenantMissing = failWhenTenantMissing;
        this.tenantSupplier = tenantSupplier;
    }

    public RlsSettings(String settingName) {
        this(settingName, true, TenantContext::getTenantId);
    }

    public static RlsSettings defaults() {
        return new RlsSettings(DEFAULT_SETTING_NAME);
    }
}
