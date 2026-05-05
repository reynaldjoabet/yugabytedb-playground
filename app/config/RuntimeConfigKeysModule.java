package config;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * For each runtime config `ScopeType` extend this module and define static ConfKeyInfo fields.
 * Define such metadata for each runtime configurable key in that scope. Such derived module will
 * contribute to Map of String (the config Key path like `yb.foo.bar`) to the ConfigKeyInfo which
 * defines the metadata for the key.
 */
abstract class RuntimeConfigKeysModule extends AbstractModule {

  @Override
  protected void configure() {
    // MapBinder<String, ConfKeyInfo<?>> mapBinder =
    //     MapBinder.newMapBinder(
    //         binder(), new TypeLiteral<String>() {}, new TypeLiteral<ConfKeyInfo<?>>() {});
    // for (Field field : this.getClass().getDeclaredFields()) {
    //   if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(ConfKeyInfo.class)) {
    //     try {
    //       ConfKeyInfo<?> keyInfo = (ConfKeyInfo<?>) field.get(null);
    //       mapBinder.addBinding(keyInfo.key).toInstance(keyInfo);
    //     } catch (IllegalAccessException e) {
    //       throw new RuntimeException(e);
    //     }
    //   }
    // }
  }
}
