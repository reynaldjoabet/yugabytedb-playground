package config;

import com.google.common.annotations.VisibleForTesting;
import models.Customer;
import models.Provider;
import models.Universe;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RuntimeConfigChangeNotifier {

  private final Map<String, List<RuntimeConfigChangeListener>> listenerMap = new HashMap<>();

  @VisibleForTesting
  public void addListener(RuntimeConfigChangeListener listener) {
    listenerMap.computeIfAbsent(listener.getKeyPath(), k -> new ArrayList<>()).add(listener);
  }
}
