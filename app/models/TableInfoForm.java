package models;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

public class TableInfoForm {

  @ApiModel(description = "Table information response")
  @Builder
  @Jacksonized
  @ToString
  public static class TableInfoResp {

    @ApiModelProperty(value = "Table ID", accessMode = READ_ONLY)
    public String tableID;

    @ApiModelProperty(value = "Table UUID", accessMode = READ_ONLY)
    public UUID tableUUID;

    @ApiModelProperty(value = "Keyspace")
    public String keySpace;

    @ApiModelProperty(value = "Table name")
    public String tableName;


    @ApiModelProperty(value = "SST size in bytes", accessMode = READ_ONLY)
    public double sizeBytes;

    @ApiModelProperty(value = "WAL size in bytes", accessMode = READ_ONLY)
    public double walSizeBytes;

    @ApiModelProperty(value = "UI_ONLY", hidden = true)
    public boolean isIndexTable;

    @ApiModelProperty(value = "Namespace or Schema")
    public String nameSpace;

    @ApiModelProperty(value = "Table space")
    public String tableSpace;

    @ApiModelProperty(value = "Parent Table UUID")
    public UUID parentTableUUID;

    @ApiModelProperty(value = "Main Table UUID of index tables")
    public UUID mainTableUUID;

    @ApiModelProperty(value = "Index Table IDs of main table")
    public List<String> indexTableIDs;

    @ApiModelProperty(value = "Postgres schema name of the table", example = "public")
    public String pgSchemaName;

    @ApiModelProperty(value = "Flag, indicating colocated table")
    public Boolean colocated;

    @ApiModelProperty(value = "Colocation parent id")
    public String colocationParentId;


    @JsonIgnore
    public String getTableId() {
      return tableID;
    }
  }

  @ApiModel(description = "Namespace information response")
  @Builder
  @Jacksonized
  public static class NamespaceInfoResp {

    @ApiModelProperty(value = "Namespace UUID", accessMode = READ_ONLY)
    public UUID namespaceUUID;

    @ApiModelProperty(value = "Namespace name")
    public String name;


    @Data
    public static class TableSizes {
      private double sstSizeBytes;
      private double walSizeBytes;
    }

    @ToString
    public static class TablePartitionInfo {

      public String tableName;

      public String schemaName;

      public String tablespace;

      public String parentTable;

      public String parentSchema;

      public String parentTablespace;

      public String keyspace;

      public TablePartitionInfo() {
      }

      public TablePartitionInfoKey getKey() {
        return new TablePartitionInfoKey(tableName, keyspace);
      }
    }

    @EqualsAndHashCode
    public static class TablePartitionInfoKey {
      private final String tableName;
      private final String keyspace;

      public TablePartitionInfoKey(String tableName, String keyspace) {
        this.tableName = tableName;
        this.keyspace = keyspace;
      }
    }
  }
}
