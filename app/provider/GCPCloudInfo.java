package provider;


import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class GCPCloudInfo implements CloudInfoInterface {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.FIELD})
  public @interface EditableInUseProvider {
    public String name();

    public boolean allowed() default true;
  }

  private static final String sharedVPCProjectKey = "GCE_SHARED_VPC_PROJECT";

  private static final Map<String, String> configKeyMap =
      ImmutableMap.of(
          "gceProject",
          "project_id",
          sharedVPCProjectKey,
          "GCE_HOST_PROJECT",
          "gceApplicationCredentialsPath",
          "GOOGLE_APPLICATION_CREDENTIALS",
          "destVpcId",
          "network"
      );

  private static final List<String> toRemoveKeyFromConfig =
      ImmutableList.of("gceApplicationCredentials", "useHostCredentials");

  private static final Map<String, String> toAddKeysInConfig =
      ImmutableMap.<String, String>builder()
          .put("client_email", "GCE_EMAIL")
          .put("project_id", "GCE_PROJECT")
          .put("auth_provider_x509_cert_url", "auth_provider_x509_cert_url")
          .put("auth_uri", "auth_uri")
          .put("client_id", "client_id")
          .put("client_x509_cert_url", "client_x509_cert_url")
          .put("private_key", "private_key")
          .put("private_key_id", "private_key_id")
          .put("token_uri", "token_uri")
          .put("type", "type")
          .build();

  private static final List<String> toMaskFieldsInCreds =
      ImmutableList.of("private_key", "private_key_id");



  @EditableInUseProvider(name = "Firewall Tags", allowed = false)
  @ApiModelProperty
  private String ybFirewallTags;

  @JsonAlias("use_host_vpc")
  @EditableInUseProvider(name = "Switching Host VPC", allowed = false)
  @ApiModelProperty
  private Boolean useHostVPC;

  @JsonAlias("use_host_credentials")
  @ApiModelProperty
  private Boolean useHostCredentials;

  @ApiModelProperty(accessMode = AccessMode.READ_ONLY)
  private String hostVpcId;

  @ApiModelProperty(
      value = "New/Existing VPC for provider creation",
      accessMode = AccessMode.READ_ONLY)
  private VPCType vpcType = VPCType.EXISTING;


  @Override
  public Map<String, String> getEnvVars() {
    return Map.of();
  }

  @Override
  public Map<String, String> getConfigMapForUIOnlyAPIs(Map<String, String> config) {
    return Map.of();
  }

  @Override
  public void mergeMaskedFields(CloudInfoInterface providerCloudInfo) {

  }

  @Override
  public void withSensitiveDataMasked() {

  }
}
