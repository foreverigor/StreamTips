package me.foreverigor.intellij.plugin.streamtips.settings;

import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compare to {@link com.intellij.openapi.editor.ex.EditorSettingsExternalizable}
 */
@Service
@State(name = "StreamTipsPluginOptions", storages = @Storage("streamtips.xml"))
public final class StreamTipsPluginSettings implements PersistentStateComponent<StreamTipsPluginSettings.StreamTipsPluginOptions> {

  public static final UINumericRange POPUPTIP_DELAY_RANGE = new UINumericRange(2500, 1, 5000);

  public static final class StreamTipsPluginOptions {
    public int POPUPTIP_DELAY = POPUPTIP_DELAY_RANGE.initial;
    public boolean EARLY_CALCULATION = false;
    public boolean CHAIN_LOADING = true;
  }
  private static boolean CHAIN_LOADING_OVERRIDE = true;


  private StreamTipsPluginOptions currentOptions = new StreamTipsPluginOptions();

  public int getPopupTipsDelay() {
    return POPUPTIP_DELAY_RANGE.fit(currentOptions.POPUPTIP_DELAY);
  }

  public void setPopupTipsDelay(int delay) {
    currentOptions.POPUPTIP_DELAY = POPUPTIP_DELAY_RANGE.fit(delay);
  }

  public boolean isShouldEarlyCalculate() {
    return currentOptions.EARLY_CALCULATION;
  }

  void setShouldEarlyCalculate(boolean val) {
    currentOptions.EARLY_CALCULATION = val;
  }

  boolean isShouldEarlyLoad() {
    return currentOptions.CHAIN_LOADING;
  }

  public boolean getShouldEarlyLoad() {
    return currentOptions.CHAIN_LOADING && CHAIN_LOADING_OVERRIDE;
  }

  public static void setOverrideDisableChainLoading() {
    CHAIN_LOADING_OVERRIDE = false;
  }

  void setShouldEarlyLoad(boolean val) {
    currentOptions.CHAIN_LOADING = val;
  }

  public static StreamTipsPluginSettings getInstance() {
    return ServiceManager.getService(StreamTipsPluginSettings.class);
  }

  @Override
  public @Nullable StreamTipsPluginSettings.StreamTipsPluginOptions getState() {
    return currentOptions;
  }

  @Override
  public void loadState(@NotNull StreamTipsPluginOptions state) {
    this.currentOptions = state;
  }
} // class StreamTipsPluginSettings
