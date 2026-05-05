package models;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import play.data.validation.Constraints;

@Entity
@Getter
@Setter
@ApiModel(
    description =
        "Access key for the cloud provider. This helps to "
            + "authenticate the user and get access to the provider.")
public class AccessKey extends Model {

  @Data
  public static class MigratedKeyInfoFields {
    // Below fields are moved to provider details
    @ApiModelProperty public String sshUser;
    @ApiModelProperty public Integer sshPort = 22;
    @ApiModelProperty public boolean airGapInstall = false;
    @ApiModelProperty public boolean passwordlessSudoAccess = true;
    @ApiModelProperty public String provisionInstanceScript = "";
    @ApiModelProperty public boolean installNodeExporter = true;
    @ApiModelProperty public Integer nodeExporterPort = 9300;
    @ApiModelProperty public String nodeExporterUser = "prometheus";
    @ApiModelProperty public boolean skipProvisioning = false;
    @ApiModelProperty public boolean setUpChrony = false;
    @ApiModelProperty public List<String> ntpServers = Collections.emptyList();

    // Indicates whether the provider was created before or after PLAT-3009
    // True if it was created after, else it was created before.
    // Dictates whether or not to show the set up NTP option in the provider UI
    @ApiModelProperty public boolean showSetUpChrony = false;

    public void mergeFrom(MigratedKeyInfoFields keyInfo) {
      sshUser = keyInfo.sshUser;
      sshPort = keyInfo.sshPort;
      airGapInstall = keyInfo.airGapInstall;
      passwordlessSudoAccess = keyInfo.passwordlessSudoAccess;
      provisionInstanceScript = keyInfo.provisionInstanceScript;
      installNodeExporter = keyInfo.installNodeExporter;
      nodeExporterPort = keyInfo.nodeExporterPort;
      nodeExporterUser = keyInfo.nodeExporterUser;
      skipProvisioning = keyInfo.skipProvisioning;
      setUpChrony = keyInfo.setUpChrony;
      showSetUpChrony = keyInfo.showSetUpChrony;
      ntpServers = keyInfo.ntpServers;
    }
  }

  @ApiModel
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class KeyInfo extends MigratedKeyInfoFields {
    @ApiModelProperty public String publicKey;
    @ApiModelProperty public String privateKey;
    @ApiModelProperty public String vaultPasswordFile;
    @ApiModelProperty public String vaultFile;
    @ApiModelProperty public boolean deleteRemote = true;
    @ApiModelProperty public String keyPairName;
    @ApiModelProperty public String sshPrivateKeyContent;
    /*
     * There are 3 different scenarios.
     * 1. Allow YW to manage keypair - We create the private key content AND upload to AWS.
     * 2. They provide their own custom keypair content/name, but it doesn't already exist in AWS.
     *    We create a private key with the same contents as what they provide and then import that
     *    to AWS account.
     * 3. They provide their own keypair content/name that matches one existing in their AWS
     *    account.
     *    a. If they let us validate, then we test the fingerprints to make sure the key content
     *       they provide matches the name of the key that already exists in AWS.
     *    b. Some customers don't want to give us permission to describe key pairs, so they need
     *       to be able to skip the validation (in this case below flag should be set, & we will
     *       trust customer with the provider keyContent)
     */
    @ApiModelProperty public boolean skipKeyValidateAndUpload = false;

    @ApiModelProperty(value = "Key Management state", accessMode = AccessMode.READ_ONLY)
    private KeyManagementState managementState = KeyManagementState.Unknown;

    public static enum KeyManagementState {
      YBAManaged,
      SelfManaged,
      Unknown
    }

    public void setManagementState(KeyManagementState state) {
      this.managementState = state;
    }

    public KeyManagementState getManagementState() {
      return this.managementState;
    }
  }
  



  // Generates a new keycode by appending the timestamp to the
  // exisitng keycode.
  public static String getNewKeyCode(String keyCode) {
    String timestamp = generateKeyCodeTimestamp();
    return String.format("%s-%s", keyCode, timestamp);
  }

  public static String generateKeyCodeTimestamp() {
    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    return sdf1.format(new Date());
  }

 



  @Column(nullable = false)
  @ManyToOne
  @JsonBackReference("provider-accessKey")
  private Provider provider;

  @Constraints.Required
  @Column(nullable = false, columnDefinition = "TEXT")
  @ApiModelProperty(value = "Cloud provider key information", required = true)
  @DbJson
  private KeyInfo keyInfo;


  // Post expiration, keys cannot be rotated into any universe and
  // will be unavailable for new universes as well
  @Column(nullable = true)
  @ApiModelProperty(
      value = "Expiration date of key",
      required = false,
      example = "2022-12-12T13:07:18Z",
      accessMode = AccessMode.READ_WRITE)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @Getter
  private Date expirationDate;




  @Column(nullable = false)
  @ApiModelProperty(
      value = "Creation date of key",
      required = false,
      example = "2022-12-12T13:07:18Z",
      accessMode = AccessMode.READ_ONLY)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @Getter
  private Date creationDate;






}
