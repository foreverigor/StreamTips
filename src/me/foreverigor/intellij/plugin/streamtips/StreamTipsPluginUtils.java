package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.openapi.diagnostic.Logger;

import me.foreverigor.intellij.plugin.streamtips.inspect.InspectionEngineBridge;
import me.foreverigor.intellij.plugin.streamtips.inspect.ManualInspectionRunner;

public class StreamTipsPluginUtils {

  final static String StreamTips = "StreamTips";

  static boolean pluginPrerequisitesAreMet() {
    boolean b = !ManualInspectionRunner.InspectionsHolder.myInspections.isEmpty() &&
            InspectionEngineBridge.InspectElementsMethodHolder.inspectElementsMethod != null &&
            IntentionPreviewPopup.PopupFieldExtractorHolder.popupFieldGetter != null;
    if (!b) {
      Logger.getInstance(StreamTips).error("One of the initialization steps required for plugin function failed, StreamTips plugin will be disabled");
    }
    return b;
  }

}
