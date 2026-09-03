package lazydevs.persistence.jdbc.rls;

/**
 * What {@link RlsDataSource} does when a connection is acquired with no tenant
 * in scope.
 *
 * <p>There are exactly two sensible answers, and neither of them is "hand back
 * an unbound connection". A PostgreSQL custom GUC cannot be returned to the
 * <em>unset</em> state once it has been written in a session - neither
 * {@code set_config(name, NULL, false)} nor {@code RESET name} nor
 * {@code DISCARD ALL} does it; all three leave the empty string. So a
 * connection that is deliberately left unbound reads back as {@code NULL} the
 * first time it is checked out of the pool and as {@code ''} every time after,
 * which makes the same tenant-less request behave differently depending on how
 * warm the pool is. That was the behaviour of {@code failWhenTenantMissing =
 * false} before this enum existed, and it is why the option is now expressed as
 * a choice between failing and binding.</p>
 *
 * @author Abhijeet Rai
 */
public enum MissingTenant {

    /**
     * Throw. The default: a tenant-less checkout is usually a wiring mistake,
     * and the database policy failing closed hides it rather than reporting it.
     */
    FAIL,

    /**
     * Bind {@link RlsSettings#getMissingTenantValue()} (the empty string unless
     * configured otherwise) and wrap the connection exactly as a tenant-scoped
     * one, so the reset-on-close guarantee still holds and the session state is
     * the same on every checkout.
     *
     * <p>This is the mode for a request that legitimately has no tenant and must
     * still see rows shared across all of them - see the shared-rows section of
     * the read-easy README. Write the policy so that an empty tenant matches
     * nothing rather than raising:</p>
     * <pre>{@code
     * using (tenant_id is null                                            -- shared
     *        or tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
     * }</pre>
     */
    BIND
}
