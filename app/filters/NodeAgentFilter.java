package filters;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import models.CloudType;
@Builder
@Getter
public class NodeAgentFilter {
  private Set<String> nodeIps;
  private CloudType cloudType;
  private UUID providerUuid;
  private UUID universeUuid;
  private UUID regionUuid;
  private UUID zoneUuid;
}
