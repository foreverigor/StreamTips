package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInspection.IntentionWrapper;
import com.intellij.codeInspection.ProblemDescriptor;
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
 * Wrapper for intention actions for when they are targeting a Java class – originally
 * {@link com.intellij.codeInspection.ex.QuickFixWrapper#isAvailable(Project, Editor, PsiFile)} was overriden to a
 * dvertise avialability for them. There was also an attempt at somehow replacing the psi class file with the generated
 * one through getFileModifierForPreview() but turns out you just can (and have to) pass the psi source file to the popup
 * processor and it will work fine. This class still exists as a marker and and as the means to get the descriptor,
 * which is needed to get the actual source file.
 */
public class ClassFileIntentionActionWrapper extends IntentionActionWrapper {

  private final ProblemDescriptor descriptor;

  public ClassFileIntentionActionWrapper(@NotNull ProblemDescriptor descriptor) {
    super(InspectionUtils.adaptToIntentionAction(descriptor), descriptor.getPsiElement().getContainingFile());
    this.descriptor = descriptor; // Descriptor is private in QuickFixWrapper, but we use it to get the generated sourceFile
  }

  public ProblemDescriptor getDescriptor() {
    return descriptor;
  }

  public boolean isAvailablea(@NotNull Project project, Editor editor, PsiFile file) {
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
