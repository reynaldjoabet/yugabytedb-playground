package models;
import io.ebean.Model;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class ActiveSessionHistory extends Model {

  private Instant sampleTime;

  private UUID universeId;

  private String nodeName;

  private UUID rootRequestId;

  private Long rpcRequestId;
  private String waitEventComponent;
  private String waitEventClass;

  private String waitEventType;
  private String waitEvent;

  private UUID topLevelNodeId;
  private Long queryId;
  private Long ysqlSessionId;

  private String clientNodeIp;

  private String waitEventAux;
  private int sampleWeight;
}
