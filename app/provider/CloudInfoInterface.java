package provider;


import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.AccessKey;
import models.AvailabilityZone;
import models.AvailabilityZoneDetails;
import models.Provider;
import models.ProviderDetails;
import models.Region;
import models.RegionDetails;
import provider.AWSCloudInfo;
import provider.AzureCloudInfo;
import provider.region.AWSRegionCloudInfo;
import provider.region.AzureRegionCloudInfo;
import provider.region.DefaultRegionCloudInfo;
import provider.region.GCPRegionCloudInfo;
import provider.region.KubernetesRegionInfo;
import provider.region.azs.DefaultAZCloudInfo;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import play.libs.Json;
import models.CloudType;
public interface CloudInfoInterface {

  public final ObjectMapper mapper = Json.mapper();

  public Map<String, String> getEnvVars();

  public Map<String, String> getConfigMapForUIOnlyAPIs(Map<String, String> config);

  public void mergeMaskedFields(CloudInfoInterface providerCloudInfo);

  public void withSensitiveDataMasked();

  public static enum VPCType {
    EXISTING,
    NEW,
    HOSTVPC // in case we want to use the same VPC as that of the host.
  }

  public static <T extends CloudInfoInterface> T get(Provider provider) {
    return get(provider, false);
  }

  public static <T extends CloudInfoInterface> T get(Region region) {
    return get(region, false);
  }

  public static <T extends CloudInfoInterface> T get(AvailabilityZone zone) {
    return get(zone, false);
  }

  
  public static <T extends CloudInfoInterface> T get(
      ProviderDetails providerDetails, Boolean maskSensitiveData, CloudType cloudType) {
    ProviderDetails.CloudInfo cloudInfo = null;//providerDetails.getCloudInfo();
  
    return null; //getCloudInfo(cloudInfo, cloudType, maskSensitiveData);
  }

  public static <T extends CloudInfoInterface> T get(Region region, Boolean maskSensitiveData) {
    RegionDetails regionDetails = region.getDetails();
    if (regionDetails == null) {
      regionDetails = new RegionDetails();
    }
    CloudType cloudType = region.getProviderCloudCode();
    return get(regionDetails, maskSensitiveData, cloudType);
  }

  public static <T extends CloudInfoInterface> T get(
      RegionDetails regionDetails, Boolean maskSensitiveData, CloudType cloudType) {
    RegionDetails.RegionCloudInfo cloudInfo =null; //regionDetails.getCloudInfo();
    if (cloudInfo == null) {
      cloudInfo = new RegionDetails.RegionCloudInfo();
      regionDetails.cloudInfo = cloudInfo;
    }
    return null;//getCloudInfo(cloudInfo, cloudType, maskSensitiveData);
  }

  public static <T extends CloudInfoInterface> T get(
      AvailabilityZone zone, Boolean maskSensitiveData) {
    AvailabilityZoneDetails azDetails = zone.getAvailabilityZoneDetails();
    if (azDetails == null) {
      azDetails = new AvailabilityZoneDetails();
    }
    CloudType cloudType = zone.getProviderCloudCode();
    return get(azDetails, maskSensitiveData, cloudType);
  }




  public static ProviderDetails maskProviderDetails(Provider provider) {
    if (Objects.isNull(provider.getDetails())) {
      return null;
    }
    JsonNode detailsJson = Json.toJson(provider.getDetails());
    ProviderDetails details = Json.fromJson(detailsJson, ProviderDetails.class);
    get(details, true, provider.getCloudCode());
    return details;
  }

  public static RegionDetails maskRegionDetails(Region region) {
    if (Objects.isNull(region.getDetails())) {
      return null;
    }
    JsonNode detailsJson = Json.toJson(region.getDetails());
    if (detailsJson.size() == 0) {
      return null;
    }
    RegionDetails details = Json.fromJson(detailsJson, RegionDetails.class);
    get(details, true, region.getProviderCloudCode());
    return details;
  }

  public static AvailabilityZoneDetails maskAvailabilityZoneDetails(AvailabilityZone zone) {
   
    JsonNode detailsJson = null;//Json.toJson(zone.getDetails());
    if (detailsJson.size() == 0) {
      return null;
    }
    AvailabilityZoneDetails details = Json.fromJson(detailsJson, AvailabilityZoneDetails.class);
    get(details, true, zone.getProviderCloudCode());
    return details;
  }

  public static void setCloudProviderInfoFromConfig(Provider provider, Map<String, String> config) {
    ProviderDetails providerDetails = provider.getDetails();
    ProviderDetails.CloudInfo cloudInfo = null;//providerDetails.getCloudInfo();
    if (cloudInfo == null) {
      cloudInfo = new ProviderDetails.CloudInfo();
      //providerDetails.setCloudInfo(cloudInfo);
    }
    CloudType cloudType = provider.getCloudCode();
    //setFromConfig(cloudInfo, config, cloudType);
  }


}
