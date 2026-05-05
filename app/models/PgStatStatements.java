package models;

import io.ebean.Model;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class PgStatStatements extends Model {

  private Instant scheduledTimestamp;

  private Instant actualTimestamp;


  private UUID universeId;

  private String nodeName;

  
  private String dbId;


  private long queryId;

  
  private double rps = Double.NaN;

 
  private double rowsAvg = Double.NaN;


  private double avgLatency = Double.NaN;


  private Double meanLatency = Double.NaN;


  private Double p90Latency = Double.NaN;


  private Double p99Latency = Double.NaN;

  private Double maxLatency = Double.NaN;
}
