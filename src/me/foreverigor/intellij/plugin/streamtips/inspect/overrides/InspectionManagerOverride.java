package me.foreverigor.intellij.plugin.streamtips.inspect.overrides;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InspectionManagerOverride extends InspectionManagerDelegateProxy {

  public InspectionManagerOverride(InspectionManager delegate) {
    super(delegate);
  }

  /**
   * Overriding only this method because it's the one that is used to create the problemDescriptor
   * {@link ProblemsHolder#registerProblem(PsiElement, String, ProblemHighlightType, TextRange, LocalQuickFix...)}
   * during our inspections
   */
  @Override
  public @NotNull ProblemDescriptor createProblemDescriptor(@NotNull PsiElement psiElement,
                                                            @Nullable("null means the text range of the element") TextRange rangeInElement,
                                                            @NotNull @InspectionMessage String descriptionTemplate,
                                                            @NotNull ProblemHighlightType highlightType,
                                                            boolean onTheFly,
                                                            LocalQuickFix... fixes) {
    boolean tooltip = highlightType != ProblemHighlightType.INFORMATION;
    return new ProblemDescriptorBaseOverride(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, rangeInElement, tooltip, onTheFly);
  }

  private static class ProblemDescriptorBaseOverride extends ProblemDescriptorBase {

    ProblemDescriptorBaseOverride(@NotNull PsiElement startElement,
                                         @NotNull PsiElement endElement,
                                         @NotNull String descriptionTemplate,
                                         @Nullable LocalQuickFix[] fixes,
                                         @NotNull ProblemHighlightType highlightType,
                                         boolean isAfterEndOfLine,
                                         @Nullable TextRange rangeInElement,
                                         boolean showTooltip,
                                         boolean onTheFly) {
      super(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, showTooltip, onTheFly);
    }

    @Override
    protected void assertPhysical(PsiElement element) {
      Logger.getInstance(ProblemDescriptorBaseOverride.class).info("Physical element assertion ignored");
    }
  }
}
