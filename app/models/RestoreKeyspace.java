package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiModel(description = "Keyspace level restores")
@Entity
@Getter
@Setter
public class RestoreKeyspace extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(RestoreKeyspace.class);
  public static final Finder<UUID, RestoreKeyspace> find =
      new Finder<UUID, RestoreKeyspace>(RestoreKeyspace.class) {};



  @ApiModelProperty(value = "Restore keyspace UUID", accessMode = READ_ONLY)
  @Id
  private UUID uuid;

  @ApiModelProperty(value = "Universe-level Restore UUID", accessMode = READ_ONLY)
  @Column(nullable = false)
  private UUID restoreUUID;

  @ApiModelProperty(value = "Restore Keyspace task UUID", accessMode = READ_ONLY)
  @Column(nullable = false)
  private UUID taskUUID;

  @ApiModelProperty(value = "Source keyspace name", accessMode = READ_ONLY)
  @Column
  private String sourceKeyspace;

  @ApiModelProperty(value = "Storage location name", accessMode = READ_ONLY)
  @Column
  private String storageLocation;

  @ApiModelProperty(value = "Target keyspace name", accessMode = READ_ONLY)
  @Column
  private String targetKeyspace;

  @ApiModelProperty(value = "Restored Table name List", accessMode = READ_ONLY)
  @Column(columnDefinition = "TEXT")
  @DbJson
  private List<String> tableNameList;



  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "RestoreKeyspace task creation time", example = "2022-12-12T13:07:18Z")
  @WhenCreated
  private Date createTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(
      value = "RestoreKeyspace task completion time",
      example = "2022-12-12T13:07:18Z")
  @WhenModified
  private Date completeTime;

  
}
