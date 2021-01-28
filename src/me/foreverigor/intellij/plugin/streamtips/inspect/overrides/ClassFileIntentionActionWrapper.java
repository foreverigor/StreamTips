package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import me.foreverigor.intellij.plugin.streamtips.inspect.InspectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of methods we are overriding are
 * {@link QuickFixWrapper#isAvailable(Project, Editor, PsiFile)} and {@link QuickFixWrapper#getFileModifierForPreview(PsiFile)}
 * */
public class ClassFileIntentionActionWrapper extends IntentionActionDelegateProxy {

  private final ProblemDescriptor descriptor;

  public ClassFileIntentionActionWrapper(@NotNull ProblemDescriptor descriptor, int fixNumber) {
    super(InspectionUtils.adaptToIntentionAction(descriptor));
    this.descriptor = descriptor; // Descriptor is private in QuickFixWrapper
  }

  public ProblemDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file instanceof ClsFileImpl) {
      PsiElement psiElement = descriptor.getPsiElement();
      return psiElement != null && psiElement.getContainingFile() instanceof PsiJavaFileImpl;
    }
    return super.isAvailable(project, editor, file);
  }

  public @Nullable FileModifier getFileModifierForPreviewa(@NotNull PsiFile target) {
    return super.getFileModifierForPreview(descriptor.getPsiElement().getContainingFile());
  }
} // class ClassFileIntentionActionWrapper
