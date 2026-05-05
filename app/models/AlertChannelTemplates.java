package models;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import models.AlertChannel.ChannelType;
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Entity
@ApiModel(description = "Alert channel templates")
public class AlertChannelTemplates extends Model {

  private static final Finder<UUID, AlertChannelTemplates> find =
      new Finder<UUID, AlertChannelTemplates>(AlertChannelTemplates.class) {};

  @Id
  @NotNull
  @ApiModelProperty(value = "Channel type", accessMode = READ_WRITE)
  private ChannelType type;

  @NotNull
  @ApiModelProperty(value = "Customer UUID", accessMode = READ_ONLY)
  private UUID customerUUID;

  @ApiModelProperty(value = "Notification title template", accessMode = READ_WRITE)
  private String titleTemplate;

  @ApiModelProperty(value = "Notification text template", accessMode = READ_WRITE)
  @NotNull
  private String textTemplate;

  public static ExpressionList<AlertChannelTemplates> createQuery() {
    return find.query().where();
  }

  @Transient @JsonIgnore private Set<String> customVariablesSet;

 
}
