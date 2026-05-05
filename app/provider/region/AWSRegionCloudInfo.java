package provider.region;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import provider.CloudInfoInterface;
@Data
@EqualsAndHashCode(callSuper = false)
public class AWSRegionCloudInfo implements CloudInfoInterface {

  @ApiModelProperty
  @JsonAlias("vnetName")
  public String vnet;

  @ApiModelProperty
  @JsonAlias("sg_id")
  public String securityGroupId;

  @ApiModelProperty(
      value =
          "<b style=\"color:#ff0000\">Deprecated since YBA version 2.20.0.</b> Use"
              + " provider.imageBundle instead",
      accessMode = AccessMode.READ_WRITE)
  public String ybImage;

  @JsonIgnore
  public Map<String, String> getEnvVars() {
    Map<String, String> envVars = new HashMap<>();

    if (securityGroupId != null) {
      envVars.put("securityGroupId", securityGroupId);
    }
  
    if (vnet != null) {
      envVars.put("vnet", vnet);
    }
    if (ybImage != null) {
      envVars.put("ybImage", ybImage);
    }

    return envVars;
  }

  @JsonIgnore
  public Map<String, String> getConfigMapForUIOnlyAPIs(Map<String, String> config) {
    return config;
  }

  @JsonIgnore
  public void withSensitiveDataMasked() {
    // pass
  }

  @JsonIgnore
  public void mergeMaskedFields(CloudInfoInterface providerCloudInfo) {
    // Pass
  }
}
