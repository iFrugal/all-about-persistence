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

    /**
     * What {@link MissingTenant#BIND} binds unless told otherwise. The empty
     * string is the right default because it is also what a released connection
     * is reset to, so a tenant-less checkout and a recycled connection present
     * the policy with identical session state.
     */
    public static final String DEFAULT_MISSING_TENANT_VALUE = "";

    /** Custom GUCs require a dot-separated two-part name. */
    private static final Pattern SETTING_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z_][A-Za-z0-9_]*");

    /** The PostgreSQL session variable the RLS policy reads. */
    private final String settingName;

    /** What to do when a connection is acquired with no tenant in scope. */
    private final MissingTenant missingTenant;

    /** The value bound under {@link MissingTenant#BIND}; ignored under FAIL. */
    private final String missingTenantValue;

    /** Where the current tenant comes from. Defaults to {@link TenantContext#getTenantId()}. */
    @ToString.Exclude
    private final Supplier<String> tenantSupplier;

    public RlsSettings(String settingName, MissingTenant missingTenant, String missingTenantValue,
                       Supplier<String> tenantSupplier) {
        if (null == settingName || !SETTING_NAME_PATTERN.matcher(settingName).matches()) {
            throw new IllegalArgumentException("RLS settingName must be a two-part custom GUC name like 'app.tenant_id', but was: " + settingName);
        }
        if (null == missingTenant) {
            throw new IllegalArgumentException("missingTenant is required");
        }
        if (null == missingTenantValue) {
            throw new IllegalArgumentException("missingTenantValue is required (use \"\" for the default)");
        }
        if (null == tenantSupplier) {
            throw new IllegalArgumentException("tenantSupplier is required");
        }
        this.settingName = settingName;
        this.missingTenant = missingTenant;
        this.missingTenantValue = missingTenantValue;
        this.tenantSupplier = tenantSupplier;
    }

    public RlsSettings(String settingName, MissingTenant missingTenant, String missingTenantValue) {
        this(settingName, missingTenant, missingTenantValue, TenantContext::getTenantId);
    }

    /** Binds {@code missingTenantValue} when no tenant is in scope. */
    public RlsSettings(String settingName, String missingTenantValue) {
        this(settingName, MissingTenant.BIND, missingTenantValue);
    }

    /** Fails loud when no tenant is in scope. */
    public RlsSettings(String settingName) {
        this(settingName, MissingTenant.FAIL, DEFAULT_MISSING_TENANT_VALUE, TenantContext::getTenantId);
    }

    /**
     * @deprecated {@code failWhenTenantMissing = false} used to hand back an
     * <em>unbound</em> connection, whose session state then depended on whether
     * that pooled connection had been used before - see {@link MissingTenant}.
     * It now maps to {@code BIND("")}, which is deterministic. Migrate to
     * {@link #RlsSettings(String, MissingTenant, String, Supplier)} and, if your
     * policy casts the setting, to the {@code nullif(..., '')::uuid} form.
     */
    @Deprecated
    public RlsSettings(String settingName, boolean failWhenTenantMissing, Supplier<String> tenantSupplier) {
        this(settingName,
                failWhenTenantMissing ? MissingTenant.FAIL : MissingTenant.BIND,
                DEFAULT_MISSING_TENANT_VALUE,
                tenantSupplier);
    }

    public static RlsSettings defaults() {
        return new RlsSettings(DEFAULT_SETTING_NAME);
    }

    /**
     * @deprecated use {@link #getMissingTenant()}.
     */
    @Deprecated
    public boolean isFailWhenTenantMissing() {
        return MissingTenant.FAIL == missingTenant;
    }
}
