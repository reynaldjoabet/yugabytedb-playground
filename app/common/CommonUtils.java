package common;

/** Helpers shared across models and providers. */
public final class CommonUtils {

  /** Number of trailing characters left readable by {@link #getMaskedValue(String)}. */
  private static final int DEFAULT_VISIBLE_LENGTH = 4;

  private static final String MASK = "****";

  private CommonUtils() {}

  public static String getMaskedValue(String value) {
    return getMaskedValue(value, DEFAULT_VISIBLE_LENGTH);
  }

  /**
   * Replaces everything but the last {@code visibleLength} characters with a fixed-width mask, so
   * that neither the value nor its length is disclosed. Masked values always contain `*`, which is
   * how callers such as {@code CloudInfoInterface.mergeMaskedFields} recognize an unedited field.
   */
  public static String getMaskedValue(String value, int visibleLength) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    if (visibleLength <= 0 || value.length() <= visibleLength) {
      return MASK;
    }
    return MASK + value.substring(value.length() - visibleLength);
  }
}
