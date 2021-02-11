package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

public class StreamTipsPluginDiagnostics {

  private static final List<Exception> pluginInitExceptions = new ArrayList<>();

  public static void recordPluginInitException(Exception exception) {
    pluginInitExceptions.add(exception);
  }

  static void handleException(Exception exception) {
    LOG.error("Exception occured", exception);
  }

  private final static Logger LOG = Logger.getInstance(StreamTipsPluginUtils.StreamTips);
}
