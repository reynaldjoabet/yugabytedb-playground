package forms.rbac;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.rbac.PermissionInfo.Action;
import models.rbac.PermissionInfo.ResourceType;

/**
 * The actions a principal may take on one resource. A null {@code resourceUUID} means the actions
 * are not tied to a specific resource, for example UNIVERSE.CREATE.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourcePermissionData {

  private ResourceType resourceType;

  private UUID resourceUUID;

  private Set<Action> actions;
}
