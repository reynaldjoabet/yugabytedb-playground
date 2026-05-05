package models;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import provider.region.AWSRegionCloudInfo;
import provider.region.AzureRegionCloudInfo;
import provider.region.DefaultRegionCloudInfo;
import provider.region.GCPRegionCloudInfo;
import provider.region.KubernetesRegionInfo;
import provider.region.azs.DefaultAZCloudInfo;
@Data
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionDetails {

  @ApiModelProperty(hidden = true)
  @Deprecated
  public String sg_id; // Security group ID.

  @Deprecated
  @ApiModelProperty(hidden = true)
  public String vnet; // Vnet key.


  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RegionCloudInfo {
    @ApiModelProperty public AWSRegionCloudInfo aws;
    @ApiModelProperty public AzureRegionCloudInfo azu;
    @ApiModelProperty public GCPRegionCloudInfo gcp;
    @ApiModelProperty public KubernetesRegionInfo kubernetes;
  }

  @ApiModelProperty public RegionCloudInfo cloudInfo;
}
