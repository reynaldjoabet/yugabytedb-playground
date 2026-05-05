package models;
import io.ebean.Finder;
import io.ebean.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.EqualsAndHashCode;

@Entity
@EqualsAndHashCode(callSuper = false)
public class LdapDnToYbaRole extends Model {

  @Id
  @Column(nullable = false, unique = true)
  public UUID uuid = UUID.randomUUID();

  @Column(nullable = false)
  public UUID customerUUID;

  @Column(nullable = false, unique = true)
  public String distinguishedName = null;

  @Column(nullable = false)
  public Role ybaRole;

  public static LdapDnToYbaRole create(UUID customerUUID, String distinguishedName, Role ybaRole) {
    LdapDnToYbaRole LdapDnToYbaRole = new LdapDnToYbaRole();
    LdapDnToYbaRole.customerUUID = customerUUID;
    LdapDnToYbaRole.distinguishedName = distinguishedName;
    LdapDnToYbaRole.ybaRole = ybaRole;
    LdapDnToYbaRole.save();
    return LdapDnToYbaRole;
  }

  public static final Finder<UUID, LdapDnToYbaRole> find =
      new Finder<UUID, LdapDnToYbaRole>(LdapDnToYbaRole.class) {};
}
