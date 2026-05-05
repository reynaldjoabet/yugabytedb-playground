

package models.rbac.routes;

import com.fasterxml.jackson.annotation.JsonProperty;
import models.rbac.Permission;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RbacPermissionDefinition {
  @JsonProperty("operator")
  Operator operator;

  @JsonProperty("rbacPermissionList")
  List<Permission> rbacPermissionList;
}
