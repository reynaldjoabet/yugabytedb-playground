package models; 

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Constraints;
import play.libs.Json;

@ApiModel(description = "SSL certificate used by the universe")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"customer_uuid", "label"}))
@Getter
@Setter
public class CertificateInfo extends Model {

  /**
   * This is the custom certificatePath information certificates received in param are converted to
   * certs and dumped in file This contains information of file path for respective certs
   */
  public static class CustomServerCertInfo {
    public String serverCert;
    public String serverKey;

    public CustomServerCertInfo() {
      this.serverCert = null;
      this.serverKey = null;
    }

    public CustomServerCertInfo(String serverCert, String serverKey) {
      this.serverCert = serverCert;
      this.serverKey = serverKey;
    }
  }

  @ApiModelProperty(value = "Certificate UUID", accessMode = READ_ONLY)
  @Constraints.Required
  @Id
  @Column(nullable = false, unique = true)
  private UUID uuid;

  @ApiModelProperty(
      value = "Customer UUID of the backup which it belongs to",
      accessMode = READ_WRITE)
  @Constraints.Required
  @Column(nullable = false)
  private UUID customerUUID;

  @ApiModelProperty(
      value = "Certificate label",
      example = "yb-admin-example",
      accessMode = READ_WRITE)
  @Column(unique = true)
  private String label;

  @Column(nullable = false)
  @JsonIgnore
  private Date startDate;

  @ApiModelProperty(
      value = "The certificate's creation date",
      accessMode = READ_WRITE,
      example = "2022-12-12T13:07:18Z")
  // @Constraints.Required
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  public Date getStartDateIso() {
    return startDate;
  }



  @JsonIgnore
  @Column(nullable = false)
  private Date expiryDate;

  @ApiModelProperty(
      value = "The certificate's expiry date",
      accessMode = READ_WRITE,
      example = "2022-12-12T13:07:18Z")
  // @Constraints.Required
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  public Date getExpiryDateIso() {
    return expiryDate;
  }

  @ApiModelProperty(
      value = "Private key path",
      example = "/opt/yugaware/.../example.key.pem",
      accessMode = READ_WRITE)
  @Column(nullable = true)
  private String privateKey;

  @ApiModelProperty(
      value = "Certificate path",
      example = "/opt/yugaware/certs/.../ca.root.cert",
      accessMode = READ_WRITE)
  @Constraints.Required
  @Column(nullable = false)
  private String certificate;

  @ApiModelProperty(
      value = "Type of the certificate",
      example = "SelfSigned",
      accessMode = READ_WRITE)
  @Constraints.Required
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private CertConfigType certType;

  @ApiModelProperty(value = "The certificate file's checksum", accessMode = READ_ONLY)
  @Column(nullable = true)
  private String checksum;

  

  @ApiModelProperty(value = "Details about the certificate", accessMode = READ_WRITE)
  @Column(columnDefinition = "TEXT", nullable = true)
  @DbJson
  // @JsonIgnore
  private JsonNode customCertInfo;

  
  private static final Finder<UUID, CertificateInfo> find =
      new Finder<UUID, CertificateInfo>(CertificateInfo.class) {};

  public static CertificateInfo get(UUID certUUID) {
    return find.byId(certUUID);
  }

  public static Optional<CertificateInfo> maybeGet(UUID certUUID) {
    // Find the CertificateInfo.
    CertificateInfo certificateInfo = find.byId(certUUID);
    if (certificateInfo == null) {
     // LOG.trace("Cannot find certificateInfo {}", certUUID);
      return Optional.empty();
    }
    return Optional.of(certificateInfo);
  }

 

  public static CertificateInfo getOrBadRequest(UUID certUUID) {
    CertificateInfo certificateInfo = get(certUUID);
    if (certificateInfo == null) {
      throw new PlatformServiceException(BAD_REQUEST, "Invalid Cert ID: " + certUUID);
    }
    return certificateInfo;
  }

  public static CertificateInfo get(String label) {
    return find.query().where().eq("label", label).findOne();
  }

  public static CertificateInfo get(UUID customerUUID, String label) {
    return find.query().where().eq("label", label).eq("customer_uuid", customerUUID).findOne();
  }

  public static List<CertificateInfo> getAll() {
    return find.query().where().findList();
  }

  public static CertificateInfo getOrBadRequest(String label) {
    CertificateInfo certificateInfo = get(label);
    if (certificateInfo == null) {
      throw new PlatformServiceException(BAD_REQUEST, "No Certificate with Label: " + label);
    }
    return certificateInfo;
  }

  public static CertificateInfo getOrBadRequest(UUID customerUUID, String label) {
    CertificateInfo certificateInfo =
        find.query().where().eq("label", label).eq("customer_uuid", customerUUID).findOne();
    if (certificateInfo == null) {
      throw new PlatformServiceException(BAD_REQUEST, "No certificate with label: " + label);
    }
    return certificateInfo;
  }

  
}
