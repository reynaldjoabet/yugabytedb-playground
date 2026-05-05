package models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.Junction;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Constraints;

@Entity
@Getter
@Setter
public class PriceComponent extends Model {
  public static final Logger LOG = LoggerFactory.getLogger(PriceComponent.class);



  // ManyToOne for provider is kept outside of PriceComponentKey
  // as ebean currently doesn't support having @ManyToOne inside @EmbeddedId
  // insertable and updatable are set to false as actual updates
  // are taken care by providerUuid parameter in PriceComponentKey
  @ManyToOne(optional = false)
  @JoinColumn(name = "provider_uuid", insertable = false, updatable = false)
  private Provider provider;




  /** The actual details of the pricing component. */
  public static class PriceDetails {

    // The unit on which the 'pricePerUnit' is based.
    public enum Unit {
      Hours,
      GBMonth,
      PIOPMonth,
      GiBpsMonth
    }

    // The price currency. Note that the case here matters as it matches AWS output.
    public enum Currency {
      USD
    }

    // The unit.
    public Unit unit;

    // Price per unit.
    public double pricePerUnit;

    // Price per hour. Derived from unit (might be per hour or per month).
    public double pricePerHour;

    // Price per day (24 hour day). Derived from unit (might be per hour or per month).
    public double pricePerDay;

    // Price per month (30 day month). Derived from unit (might be per hour or per month).
    public double pricePerMonth;

    // Currency.
    public Currency currency;

    // Keeping these around for now as they seem useful.
    public String effectiveDate;
    public String description;

    public void setUnitFromString(String unit) {
      switch (unit.toUpperCase()) {
        case "GB-MO":
        case "GBMONTH":
          this.unit = Unit.GBMonth;
          break;
        case "HRS":
        case "HOURS":
          this.unit = Unit.Hours;
          break;
        case "IOPS-MO":
          this.unit = Unit.PIOPMonth;
          break;
        case "GIBPS-MO":
          this.unit = Unit.GiBpsMonth;
          break;
        default:
          LOG.error("Invalid price unit provided: " + unit);
          break;
      }
    }
  }
}
