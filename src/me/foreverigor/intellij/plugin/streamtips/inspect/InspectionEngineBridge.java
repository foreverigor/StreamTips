package me.foreverigor.intellij.plugin.streamtips.inspect;

import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.foreverigor.intellij.plugin.streamtips.inspect.InspectionEngineBridge.InspectElementsMethodHolder.inspectElementsMethod;

/**
 * Exposes the pkg-private method
 * {@link InspectionEngine#inspectElements(List, PsiFile, InspectionManager, boolean, ProgressIndicator, List, Set)}
 * which is needed for ManualInspectionRunner
 */
public class InspectionEngineBridge {

    @SuppressWarnings("unchecked")
    static @NotNull Map<String, List<ProblemDescriptor>> runInspectElements(@NotNull List<? extends LocalInspectionToolWrapper> toolWrappers,
                                                                                   final @NotNull PsiFile file,
                                                                                   final @NotNull InspectionManager iManager,
                                                                                   final boolean isOnTheFly,
                                                                                   @NotNull ProgressIndicator indicator,
                                                                                   final @NotNull List<? extends PsiElement> elements,
                                                                                   final @NotNull Set<String> elementDialectIds) throws Exception {
        return (Map<String, List<ProblemDescriptor>>) inspectElementsMethod.invoke(null, toolWrappers, file, iManager, isOnTheFly, indicator, elements, elementDialectIds);
    }

    static class InspectElementsMethodHolder {
        static final Method inspectElementsMethod = getInspectElementsMethod();
    }

    private static final String methodName = "inspectElements";
    private static final Class<?>[] paramTypes = {
            List.class,
            PsiFile.class,
            InspectionManager.class,
            boolean.class,
            ProgressIndicator.class,
            List.class,
            Set.class
    };

    private static Method getInspectElementsMethod() {
        try {
            Method inspectElements = InspectionEngine.class.getDeclaredMethod(methodName, paramTypes);
            inspectElements.setAccessible(true);
            return inspectElements;
        } catch (Exception e) {}
        return null;
    }
}