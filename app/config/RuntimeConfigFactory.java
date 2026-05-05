package config;

import com.typesafe.config.Config;
import models.Customer;
import models.Provider;
import models.Universe;

public interface RuntimeConfigFactory {
  Config forCustomer(Customer customer);

  Config forUniverse(Universe universe);

  Config forProvider(Provider provider);

  Config globalRuntimeConf();

  Config staticApplicationConf();
}
