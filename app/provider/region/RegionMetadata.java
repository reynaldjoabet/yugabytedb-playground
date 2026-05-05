

package provider.region;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RegionMetadata {
  private Map<String, RegionMetadataInfo> regionMetadata;

  @Data
  public static class RegionMetadataInfo {
    private String name;
    private double latitude;
    private double longitude;

    @JsonAlias("availabilty_zones")
    private List<String> availabilityZones;
  }
}
