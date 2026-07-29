package config;

import com.typesafe.config.Config;
import java.util.function.BiFunction;

/**
 * Metadata for a single runtime configurable key: where it lives in the config, how to read it as
 * its declared type, and what to fall back to when the path is not set.
 *
 * @param <T> the type the key resolves to.
 */
public class ConfKeyInfo<T> {

  final String key;

  private final BiFunction<Config, String, T> reader;

  private final T defaultValue;

  ConfKeyInfo(String key, BiFunction<Config, String, T> reader, T defaultValue) {
    this.key = key;
    this.reader = reader;
    this.defaultValue = defaultValue;
  }

  static ConfKeyInfo<Boolean> booleanKey(String key, boolean defaultValue) {
    return new ConfKeyInfo<>(key, Config::getBoolean, defaultValue);
  }

  public String getKey() {
    return key;
  }

  public T getValue(Config config) {
    return config.hasPath(key) ? reader.apply(config, key) : defaultValue;
  }

  @Override
  public String toString() {
    return key;
  }
}
