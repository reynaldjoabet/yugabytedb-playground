
package models;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import play.libs.Json;

@Slf4j

public class CloudBootstrap {

    public static class Params {
    }

    // Class to encapsulate custom network bootstrap overrides per region.
    public static class PerRegionMetadata {
        // Custom VPC ID to use for this region
        // Default: created by YB.
        // Required: True for custom input, False for YW managed.
        public String vpcId;

        // Custom CIDR to use for the VPC, if YB is creating it.
        // Default: chosen by YB.
        // Required: False.
        public String vpcCidr;

        // Custom map from AZ name to Subnet ID for AWS.
        // Default: created by YB.
        // Required: True for custom input, False for YW managed.
        public Map<String, String> azToSubnetIds;

        // Custom map from AZ name to Subnet ID for AWS.
        // Default: Empty
        // Required: False for custom input, False for YW managed.
        public Map<String, String> azToSecondarySubnetIds = null;

        // Region Subnet ID for GCP.
        // Default: created by YB.
        // Required: True for custom input, False for YW managed.
        public String subnetId;

        // Region Secondary Subnet ID for GCP.
        // Default: Null
        // Required: False for custom input, False for YW managed.
        public String secondarySubnetId = null;

        // TODO(bogdan): does this not need a custom SSH user as well???
        // Custom AMI ID to use for YB nodes.
        // Default: hardcoded in devops.
        // Required: False.
        public String customImageId;

        // Custom SG ID to use for the YB nodes.
        // Default: created by YB.
        // Required: True for custom input, False for YW managed.
        public String customSecurityGroupId;

        // Required for configuring region for onprem provider.
        public String regionName;
        public double latitude;
        public double longitude;
        // List of zones for regions, to be used for only onprem usecase.
        public List<AvailabilityZone> azList;

        // Instance template to use for new YB nodes.
        // Default: Null.
        // Required: False.
        public String instanceTemplate = null;


        public String networkRGOverride;

        public String resourceGroupOverride;


        // Map from region name to metadata.
        public Map<String, PerRegionMetadata> perRegionMetadata = new HashMap<>();

        // Custom keypair name to use when spinning up YB nodes.
        // Default: created and managed by YB.
        public String keyPairName = null;

        // Custom SSH private key component.
        // Default: created and managed by YB.
        public String sshPrivateKeyContent = null;

        // Custom SSH user to login to machines.
        // Default: created and managed by YB.
        public String sshUser = null;

        // Whether provider should use airgapped install.
        // Default: false.
        public boolean airGapInstall = false;

        // Port to open for connections on the instance.
        public Integer sshPort = 22;

        public String hostVpcId = null;
        public String hostVpcRegion = null;
        public String destVpcId = null;
        public boolean createNewVpc = false;

        // Dictates whether or not NTP should be configured on newly provisioned nodes.
        public boolean setUpChrony = false;

        // Dictates which NTP servers should be configured on newly provisioned nodes.
        public List<String> ntpServers = new ArrayList<>();

        // Indicates whether the provider was created before or after PLAT-3009.
        // True if it was created after, else it was created before.
        // Dictates whether or not to show the set up NTP option in the provider UI.
        public boolean showSetUpChrony = true;

        public List<ImageBundle> imageBundles;

        // used for onprem nodes for the cases when manual provision is set.
        public boolean skipProvisioning = false;

        // used for skipping the key validation & upload for AWS provider.
        // See, AccessKey.KeyInfo for detailed summary on usage.
        public boolean skipKeyValidateAndUpload = false;

        // K8s provider specific params.
        public Provider reqProviderEbean;
    }
}
  

