package me.foreverigor.intellij.plugin.streamtips.inspect;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import me.foreverigor.intellij.plugin.streamtips.inspect.overrides.IntentionActionWrapper;

public class InspectionUtils {

  @NotNull
  public static IntentionAction adaptToIntentionAction(@NotNull ProblemDescriptor problemDescriptor) {
    return QuickFixWrapper.wrap(problemDescriptor, 0);
  }

  @NotNull
  public static IntentionAction adaptToIntentionAction(@NotNull ProblemDescriptor problemDescriptor, @NotNull PsiFile file) {
    return new IntentionActionWrapper(QuickFixWrapper.wrap(problemDescriptor,0), file);
  }
  private InspectionUtils() {}
}
