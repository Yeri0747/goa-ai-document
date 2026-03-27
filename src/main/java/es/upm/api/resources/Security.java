package es.upm.api.resources;

public final class Security {
    public static final String AUTHENTICATED = "isAuthenticated()";
    public static final String ADMIN_MANAGER_OPERATOR = "hasAnyRole('admin','manager','operator')";
    public static final String ADMIN_MANAGER_OPERATOR_URL_TOKEN = "hasAnyRole('admin','manager','operator','url_token')";
    public static final String ADMIN_MANAGER_OPERATOR_CUSTOMER = "hasAnyRole('admin','manager','operator','customer')";
    public static final String CUSTOMER_OWNER = "hasRole('customer') and #id == authentication.name";
    public static final String ALL = "permitAll()";
    public static final String OR = " or ";
    public static final String AND = " and ";

    private Security() {
        // Forbidden
    }
}
