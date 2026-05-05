
package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Junction;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Constraints;
import play.libs.Json;

@ApiModel(description = "Information about an instance")
@Entity
@Getter
@Setter
public class InstanceType extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(InstanceType.class);

  // todo: https://yugabyte.atlassian.net/browse/PLAT-10505
  private static final Pattern AZU_NO_LOCAL_DISK =
      Pattern.compile("Standard_(D|E)[0-9]*as\\_v5|Standard_D[0-9]*s\\_v5|Standard_D[0-9]*s\\_v4");

  private static final List<String> AWS_INSTANCE_PREFIXES_SUPPORTED =
      ImmutableList.of(
          "m3.", "c5.", "c5d.", "c4.", "c3.", "i3.", "m4.", "m5.", "m5a.", "m6i.", "m6a.", "m7i.",
          "m7a.", "c6i.", "c6a.", "c7a.", "c7i.");
  private static final List<String> GRAVITON_AWS_INSTANCE_PREFIXES_SUPPORTED =
      ImmutableList.of("m6g.", "c6gd.", "c6g.", "t4g.");
  private static final List<String> CLOUD_AWS_INSTANCE_PREFIXES_SUPPORTED =
      ImmutableList.of(
          "m3.", "c5.", "c5d.", "c4.", "c3.", "i3.", "t2.", "t3.", "t4g.", "m6i.", "m5.");

  static final String YB_AWS_DEFAULT_VOLUME_COUNT_KEY = "yb.aws.default_volume_count";
  static final String YB_AWS_DEFAULT_VOLUME_SIZE_GB_KEY = "yb.aws.default_volume_size_gb";

  public enum VolumeType {
    @EnumValue("EBS")
    EBS,

    @EnumValue("SSD")
    SSD,

    @EnumValue("HDD")
    HDD,

    @EnumValue("NVME")
    NVME
  }

  // ManyToOne for provider is kept outside of InstanceTypeKey
  // as ebean currently doesn't support having @ManyToOne inside @EmbeddedId
  // insertable and updatable are set to false as actual updates
  // are taken care by providerUuid parameter in InstanceTypeKey
  // This is currently not used directly, but in order for ebean to be able
  // to save instances of this model, this association has to be bidirectional
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_uuid", insertable = false, updatable = false)
  @JsonIgnore
  private Provider provider;

  @ApiModelProperty(value = "True if the instance is active", accessMode = READ_ONLY)
  @Constraints.Required
  @Column(nullable = false, columnDefinition = "boolean default true")
  @Getter
  @Setter
  private boolean active = true;

  @ApiModelProperty(value = "The instance's number of CPU cores", accessMode = READ_WRITE)
  @Constraints.Required
  @Column(nullable = false, columnDefinition = "float")
  private Double numCores;

  @ApiModelProperty(value = "The instance's memory size, in gigabytes", accessMode = READ_WRITE)
  @Constraints.Required
  @Column(nullable = false, columnDefinition = "float")
  private Double memSizeGB;






  public static InstanceType upsert(
      UUID providerUuid,
      String instanceTypeCode,
      Integer numCores,
      Double memSize,
      InstanceTypeDetails instanceTypeDetails) {
    return upsert(providerUuid, instanceTypeCode, (int) numCores, memSize, instanceTypeDetails);
  }

  




  public static InstanceType createWithMetadata(
      UUID providerUuid, String instanceTypeCode, JsonNode metadata) {
    return upsert(
        providerUuid,
        instanceTypeCode,
        Integer.parseInt(metadata.get("numCores").toString()),
        Double.parseDouble(metadata.get("memSizeGB").toString()),
        Json.fromJson(metadata.get("instanceTypeDetails"), InstanceTypeDetails.class));
  }

  

  private static boolean compareVolumeDetailsList(
      List<VolumeDetails> volDetails1, List<VolumeDetails> volDetails2) {
    if (CollectionUtils.isEmpty(volDetails1)) {
      return CollectionUtils.isEmpty(volDetails2);
    }
    if (CollectionUtils.isEmpty(volDetails2)) {
      return CollectionUtils.isEmpty(volDetails1);
    }
    if (volDetails1.size() != volDetails2.size()) {
      return false;
    }
    Comparator<VolumeDetails> comparator =
        (v1, v2) -> {
          int res = Integer.compare(v1.volumeType.ordinal(), v2.volumeType.ordinal());
          if (res != 0) {
            return res;
          }
          res = Integer.compare(v1.volumeSizeGB, v2.volumeSizeGB);
          if (res != 0) {
            return res;
          }
          return StringUtils.compare(v1.mountPath, v2.mountPath);
        };
    Collections.sort(volDetails1, comparator);
    Collections.sort(volDetails2, comparator);
    for (int idx = 0; idx < volDetails1.size(); idx++) {
      if (!volDetails1.get(idx).equals(volDetails2.get(idx))) {
        return false;
      }
    }
    return true;
  }



  /** Default details for volumes attached to this instance. */
  @EqualsAndHashCode
  @ToString
  public static class VolumeDetails {
    public Integer volumeSizeGB;
    public VolumeType volumeType;
    public String mountPath;
  }

  public static class InstanceTypeDetails {
    public static final int DEFAULT_VOLUME_COUNT = 1;
    public static final int DEFAULT_GCP_VOLUME_SIZE_GB = 375;
    public static final int DEFAULT_AZU_VOLUME_SIZE_GB = 250;

    // These instance type codes are typically the ones provided by the cloud vendor.
    @ApiModelProperty(
        hidden = true,
        required = false,
        value = "Decreasing priority list of cloud or vendor instance type code")
    @JsonInclude(Include.NON_EMPTY)
    public List<String> cloudInstanceTypeCodes = new LinkedList<>();

    @ApiModelProperty(value = "Volume Details for the instance.")
    public List<VolumeDetails> volumeDetailsList = new LinkedList<>();

    public void setVolumeDetailsList(int volumeCount, int volumeSizeGB, VolumeType volumeType) {
      for (int i = 0; i < volumeCount; i++) {
        VolumeDetails volumeDetails = new VolumeDetails();
        volumeDetails.volumeSizeGB = volumeSizeGB;
        volumeDetails.volumeType = volumeType;
        volumeDetailsList.add(volumeDetails);
      }
      setDefaultMountPaths();
    }

    public void setDefaultMountPaths() {
      for (int idx = 0; idx < volumeDetailsList.size(); ++idx) {
        volumeDetailsList.get(idx).mountPath = String.format("/mnt/d%d", idx);
      }
    }

    public static InstanceTypeDetails createGCPDefault() {
      InstanceTypeDetails instanceTypeDetails = new InstanceTypeDetails();
      instanceTypeDetails.setVolumeDetailsList(
          DEFAULT_VOLUME_COUNT, DEFAULT_GCP_VOLUME_SIZE_GB, VolumeType.SSD);
      return instanceTypeDetails;
    }

    public static InstanceTypeDetails createAZUDefault() {
      InstanceTypeDetails instanceTypeDetails = new InstanceTypeDetails();
      instanceTypeDetails.setVolumeDetailsList(
          DEFAULT_VOLUME_COUNT, DEFAULT_AZU_VOLUME_SIZE_GB, VolumeType.SSD);
      return instanceTypeDetails;
    }

    public static InstanceTypeDetails createGCPInstanceTypeDetails(VolumeType volumeType) {
      InstanceTypeDetails instanceTypeDetails = new InstanceTypeDetails();
      instanceTypeDetails.setVolumeDetailsList(
          DEFAULT_VOLUME_COUNT, DEFAULT_GCP_VOLUME_SIZE_GB, volumeType);
      return instanceTypeDetails;
    }
  }
}
