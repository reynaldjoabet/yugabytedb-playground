package config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;

/**
 * Reads runtime configurable keys. Values come from the static application config; keys that are
 * not set fall back to the default declared on the {@link ConfKeyInfo}.
 */
@Singleton
public class RuntimeConfGetter {

  private final Config config;

  @Inject
  public RuntimeConfGetter(Config config) {
    this.config = config;
  }

  public <T> T getGlobalConf(ConfKeyInfo<T> keyInfo) {
    return keyInfo.getValue(config);
  }
}
