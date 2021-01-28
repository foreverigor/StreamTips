package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InspectionManagerDelegateProxy extends InspectionManager {

  public InspectionManagerDelegateProxy(InspectionManager delegate) {
    this.delegate = delegate;
  }

  private final InspectionManager delegate;

  public @NotNull Project getProject() {
    return delegate.getProject();
  }

  @Contract(pure = true)
  public @NotNull CommonProblemDescriptor createProblemDescriptor(@NotNull @InspectionMessage String descriptionTemplate, @Nullable QuickFix... fixes) {
    return delegate.createProblemDescriptor(descriptionTemplate, fixes);
  }

  @Contract(pure = true)
  public @NotNull ModuleProblemDescriptor createProblemDescriptor(@NotNull @InspectionMessage String descriptionTemplate, @NotNull Module module, @Nullable QuickFix... fixes) {
    return delegate.createProblemDescriptor(descriptionTemplate, module, fixes);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, @Nullable LocalQuickFix fix, @NotNull ProblemHighlightType highlightType, boolean onTheFly) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, fix, highlightType, onTheFly);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, boolean onTheFly, LocalQuickFix[] fixes, @NotNull ProblemHighlightType highlightType) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, onTheFly, fixes, highlightType);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, @Nullable LocalQuickFix[] fixes, @NotNull ProblemHighlightType highlightType, boolean onTheFly, boolean isAfterEndOfLine) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, onTheFly, isAfterEndOfLine);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull @InspectionMessage String descriptionTemplate, @NotNull ProblemHighlightType highlightType, boolean onTheFly, LocalQuickFix... fixes) {
    return delegate.createProblemDescriptor(startElement, endElement, descriptionTemplate, highlightType, onTheFly, fixes);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @Nullable("null means the text range of the element") TextRange rangeInElement, @NotNull @InspectionMessage String descriptionTemplate, @NotNull ProblemHighlightType highlightType, boolean onTheFly, LocalQuickFix... fixes) {
    return delegate.createProblemDescriptor(psiElement, rangeInElement, descriptionTemplate, highlightType, onTheFly, fixes);
  }

  @Contract(pure = true)
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, boolean showTooltip, @NotNull ProblemHighlightType highlightType, boolean onTheFly, LocalQuickFix... fixes) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, showTooltip, highlightType, onTheFly, fixes);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, @Nullable LocalQuickFix fix, @NotNull ProblemHighlightType highlightType) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, fix, highlightType);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, LocalQuickFix[] fixes, @NotNull ProblemHighlightType highlightType) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, @NotNull @InspectionMessage String descriptionTemplate, LocalQuickFix[] fixes, @NotNull ProblemHighlightType highlightType, boolean isAfterEndOfLine) {
    return delegate.createProblemDescriptor(psiElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull @InspectionMessage String descriptionTemplate, @NotNull ProblemHighlightType highlightType, LocalQuickFix... fixes) {
    return delegate.createProblemDescriptor(startElement, endElement, descriptionTemplate, highlightType, fixes);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement, TextRange rangeInElement, @NotNull @InspectionMessage String descriptionTemplate, @NotNull ProblemHighlightType highlightType, LocalQuickFix... fixes) {
    return delegate.createProblemDescriptor(psiElement, rangeInElement, descriptionTemplate, highlightType, fixes);
  }

  @Contract(pure = true)
  @Deprecated
  public @NotNull GlobalInspectionContext createNewGlobalContext(boolean reuse) {
    return delegate.createNewGlobalContext(reuse);
  }

  @Contract(pure = true)
  public @NotNull GlobalInspectionContext createNewGlobalContext() {
    return delegate.createNewGlobalContext();
  }
}
