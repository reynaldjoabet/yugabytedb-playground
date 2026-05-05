
package models;
import static org.reflections.Reflections.log;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.ebean.Finder;
import io.ebean.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApiModel(description = "Custom CA certificate")
@Entity
@Getter
@Setter
@Slf4j
public class CustomCaCertificateInfo extends Model {

  private String name;

  @Id private UUID id;

  private UUID customerId;

  @ApiModelProperty(
      value = "Path to CA Certificate",
      example = "/opt/yugaware/certs/trust-store/<ID>/ca.root.cert")
  private String contents;

  @ApiModelProperty(value = "Start date of certificate validity.", example = "2023-10-12T13:07:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date startDate;

  @ApiModelProperty(value = "End date of certificate validity.", example = "2024-07-12T13:07:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date expiryDate;

  private boolean active;

  @ApiModelProperty(value = "Date when certificate was added.", example = "2023-11-10T15:09:18Z")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private Date createdTime;

  private static final Finder<UUID, CustomCaCertificateInfo> finder =
      new Finder<>(CustomCaCertificateInfo.class) {};



  public static boolean delete(UUID customerId, UUID certId) {
    CustomCaCertificateInfo cert = null;//get(customerId, certId, false);
    if (cert == null) {
      log.error(String.format("Certificate %s does not exist", certId));
      return false;
    }
    return cert.delete();
  }

  
  public static CustomCaCertificateInfo getByName(String name) {
    log.debug("Getting certificate by name {}", name);
    List<CustomCaCertificateInfo> certs =
        finder.query().where().eq("name", name).eq("active", true).findList();
    return certs != null && !certs.isEmpty() ? certs.get(0) : null;
  }

  private static String getCACertData(String cert_path) {
    log.debug("Getting certificate string from {}", cert_path);
    byte[] byteData = FileData.getDecodedData(cert_path);
    String strCert = new String(byteData, StandardCharsets.UTF_8);
    return strCert;
  }
}
