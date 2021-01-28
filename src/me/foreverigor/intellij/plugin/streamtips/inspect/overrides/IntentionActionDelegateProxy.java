package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * We can't extend from IntentionActionDelegate because those get unwrapped in
 * {@link com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler#availableFor(PsiFile, Editor, IntentionAction)},
 * nullifying all overrides
 */
class IntentionActionDelegateProxy implements IntentionAction {

  private final IntentionAction delegate;

  IntentionActionDelegateProxy(IntentionAction delegate) {
    this.delegate = delegate;
  }

  @Override
  @NotNull
  @IntentionName
  public String getText() {
    return delegate.getText();
  }

  @Override
  @NotNull
  @IntentionFamilyName
  public String getFamilyName() {
    return delegate.getFamilyName();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return delegate.isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    delegate.invoke(project, editor, file);
  }

  @Override
  public boolean startInWriteAction() {
    return delegate.startInWriteAction();
  }

  @Override
  public @Nullable PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
    return delegate.getElementToMakeWritable(currentFile);
  }

  @Override
  public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
    return delegate.getFileModifierForPreview(target);
  }
} // class IntentionActionDelegateProxy
