package lazydevs.persistence.connection.multitenant;

public class TenantContext {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<String>();

    public static String getTenantId() {
        return CONTEXT_HOLDER.get();
    }

    public static void setTenantId(String tenantId) {
        CONTEXT_HOLDER.set(tenantId);
    }

    public static void reset(){
        CONTEXT_HOLDER.remove();
    }

}
