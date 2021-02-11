package me.foreverigor.intellij.plugin.streamtips.inspect;

import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import me.foreverigor.intellij.platform.annotations.Required;
import me.foreverigor.intellij.plugin.streamtips.StreamTipsPluginDiagnostics;
import me.foreverigor.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.foreverigor.intellij.plugin.streamtips.inspect.InspectionEngineBridge.InspectElementsMethodHolder.inspectElementsMethod;

/**
 * Exposes the pkg-private method
 * {@link InspectionEngine#inspectElements(List, PsiFile, InspectionManager, boolean, ProgressIndicator, List, Set)}
 * which is needed for ManualInspectionRunner. <p>
 * There could be another, better way of accessing the method (loading this class in the right ClassLoader or reloading
 * InspectionEnging into the plugin loader) but I'm not sure how it could be done, just calling the method from a plugin
 * class in the same package gives an IllegalAccessException
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

    public static class InspectElementsMethodHolder {
        @Required
        public static final Method inspectElementsMethod = getInspectElementsMethod();
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
            return ReflectionUtils.getMethod(InspectionEngine.class, methodName, paramTypes);
        } catch (Exception e) {
            StreamTipsPluginDiagnostics.recordPluginInitException(e);
        }
        return null;
    }
}
