package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class ImageBundle extends Model {

  public static enum ImageBundleType {
    YBA_ACTIVE,
    YBA_DEPRECATED,
    CUSTOM
  };

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class Metadata {
    private ImageBundleType type;
    private String version;
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NodeProperties {
    private String machineImage;
    private String sshUser;
    private Integer sshPort;
  }

  @ApiModelProperty(value = "Image Bundle UUID", accessMode = READ_ONLY)
  @Id
  private UUID uuid;

  @ApiModelProperty(value = "Image Bundle Name")
  private String name;

  @ManyToOne
  @JsonBackReference("provider-image-bundles")
  private Provider provider;



  @ApiModelProperty(
      value = "Default Image Bundle. A provider can have two defaults, one per architecture")
  @Column(name = "is_default")
  private Boolean useAsDefault = false;

  @ApiModelProperty(value = "Metadata for imageBundle")
  @DbJson
  private Metadata metadata = new Metadata();

  @ApiModelProperty(value = "Is the ImageBundle Active")
  private Boolean active = true;

  public static final Finder<UUID, ImageBundle> find =
      new Finder<UUID, ImageBundle>(ImageBundle.class) {};

  public static ImageBundle getOrBadRequest(UUID providerUUID, UUID imageBundleUUID) {
    ImageBundle bundle = ImageBundle.get(providerUUID, imageBundleUUID);
    if (bundle == null) {
      throw new PlatformServiceException(
          BAD_REQUEST, "Invalid ImageBundle UUID: " + imageBundleUUID);
    }
    return bundle;
  }

  public static ImageBundle getOrBadRequest(UUID imageBundleUUID) {
    ImageBundle bundle = ImageBundle.get(imageBundleUUID);
    if (bundle == null) {
      throw new PlatformServiceException(
          BAD_REQUEST, "Invalid ImageBundle UUID: " + imageBundleUUID);
    }
    return bundle;
  }

  public static ImageBundle get(UUID imageBundleUUID) {
    return find.byId(imageBundleUUID);
  }

  public static ImageBundle get(UUID providerUUID, UUID imageBundleUUID) {
    return find.query().where().eq("provider_uuid", providerUUID).idEq(imageBundleUUID).findOne();
  }

  public static List<ImageBundle> getAll(UUID providerUUID) {
    return find.query().where().eq("provider_uuid", providerUUID).findList();
  }



  public static List<ImageBundle> getDefaultForProvider(UUID providerUUID) {
    // At a given time, two defaults can exist in image bundle one for `x86` & other for `arm`.
    return find.query().where().eq("provider_uuid", providerUUID).eq("is_default", true).findList();
  }

  
  }

