package models.helpers;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.rbac.Permission;

/** Serialized into the `permission_details` JSON column of a {@link models.rbac.Role}. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDetails {

  private Set<Permission> permissionList = new HashSet<>();
}
