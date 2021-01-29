package me.foreverigor.intellij.plugin.streamtips;

public class StreamTipsPluginUtils {

  private final static int POPUPTIP_DELAY = 2500;
  private final static boolean EARLY_CALCULATION = true;

  static int getPopuptipDelay() {
    return POPUPTIP_DELAY;
  }

  static boolean getShouldEarlyCalculate() {
    return EARLY_CALCULATION;
  }
}
