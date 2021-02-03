package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.IntentionWrapper;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntentionActionWrapper extends IntentionWrapper {
  private final IntentionAction delegateAction;

  public IntentionActionWrapper(@NotNull IntentionAction action, @NotNull PsiFile file) {
    super(action, file);
    this.delegateAction = action;
  }

  @Override
  public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
    return delegateAction.getFileModifierForPreview(target); // Has to be from delegate
  }

}
