
package models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import provider.AWSCloudInfo;
import provider.AzureCloudInfo;
import provider.GCPCloudInfo;
import provider.KubernetesInfo;
import provider.LocalCloudInfo;
import provider.OnPremCloudInfo;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
// Excluding cloudInfo as cloudInfo has its own equals & hashCode implementation.
//@EqualsAndHashCode(
//    callSuper = true,
//    exclude = {"cloudInfo"})
//@ToString(callSuper = true)
public class ProviderDetails  {

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CloudInfo {
    @ApiModelProperty public AWSCloudInfo aws;
    @ApiModelProperty public AzureCloudInfo azu;
    @ApiModelProperty public GCPCloudInfo gcp;
    @ApiModelProperty public KubernetesInfo kubernetes;
    @ApiModelProperty public OnPremCloudInfo onprem;
    @ApiModelProperty public LocalCloudInfo local;
  }

  @ApiModelProperty private CloudInfo cloudInfo;

  // Flag to enable node agent for this provider depending on the runtime config settings.
  @ApiModelProperty(accessMode = AccessMode.READ_ONLY)
  public boolean enableNodeAgent;


}
