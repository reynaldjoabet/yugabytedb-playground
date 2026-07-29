package config;

public class GlobalConfKeys {
    public static Object useOauth;

    /** Gate for the fine-grained RBAC authorization path. */
    public static final ConfKeyInfo<Boolean> useNewRbacAuthz =
            ConfKeyInfo.booleanKey("yb.rbac.use_new_authz", false);
}
