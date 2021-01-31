package me.foreverigor.intellij.plugin.streamtips;

public class StreamTipsPluginUtils {

  private final static int POPUPTIP_DELAY = 2500;
  private final static boolean EARLY_CALCULATION = true;
  private final static boolean CHAIN_LOADING = true;
  private static boolean CHAIN_LOADING_OVERRIDE = true;

  static int getPopuptipDelay() {
    return POPUPTIP_DELAY;
  }

  static boolean getShouldEarlyCalculate() {
    return EARLY_CALCULATION;
  }

  static void setOverrideDisableChainLoading() {
    CHAIN_LOADING_OVERRIDE = false;
  }

  static boolean getDrawPopupOnPreviewAvailable() {
    return CHAIN_LOADING && CHAIN_LOADING_OVERRIDE;
  }
}
