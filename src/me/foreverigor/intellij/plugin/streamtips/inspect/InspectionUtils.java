package me.foreverigor.intellij.plugin.streamtips.inspect;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import org.jetbrains.annotations.NotNull;

public class InspectionUtils {

  @NotNull
  public static IntentionAction adaptToIntentionAction(@NotNull ProblemDescriptor problemDescriptor) {
    return QuickFixWrapper.wrap(problemDescriptor, 0);
  }

  private InspectionUtils() {}
}
