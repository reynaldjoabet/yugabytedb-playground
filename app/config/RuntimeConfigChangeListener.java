package config;
import models.Customer;
import models.Provider;
import models.Universe;

public interface RuntimeConfigChangeListener {
  String getKeyPath();

  default void processGlobal() {
    // Do nothing by default
  }

  default void processCustomer(Customer customer) {
    // Do nothing by default
  }

  default void processUniverse(Universe universe) {
    // Do nothing by default
  }

  default void processProvider(Provider provider) {
    // Do nothing by default
  }
}
