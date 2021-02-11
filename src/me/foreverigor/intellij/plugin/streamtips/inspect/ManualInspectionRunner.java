package me.foreverigor.intellij.plugin.streamtips.inspect;

import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import com.intellij.codeInspection.streamToLoop.StreamToLoopInspection;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;

import me.foreverigor.intellij.platform.annotations.Required;
import me.foreverigor.intellij.plugin.streamtips.StreamTipsPluginDiagnostics;
import me.foreverigor.intellij.plugin.streamtips.Utils;
import me.foreverigor.intellij.plugin.streamtips.inspect.overrides.ClassFileIntentionActionWrapper;
import me.foreverigor.intellij.plugin.streamtips.inspect.overrides.InspectionManagerOverride;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static me.foreverigor.intellij.plugin.streamtips.Utils.getFirst;

/**
 * This class is responsible for manually running only a select view of inspections {currently only {@link StreamToLoopInspection}}
 * on the selected psi element. To do this, it replicates the actions {@link com.intellij.codeInsight.daemon.impl.DoNotShowInspectionIntentionMenuContributor}
 * and {@link com.intellij.codeInsight.daemon.impl.LocalInspectionsPass} (as part of {@link com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl})
 * do when running inspections. Both of these actually use the more specific {@link InspectionEngine#createVisitorAndAcceptElements(LocalInspectionTool, ProblemsHolder, boolean, LocalInspectionToolSession, List)}
 * method in their implementation, but there is also the package-private inspectElements() which also ends up calling the same
 * method but returns the result in a convenient format and so I use it instead.
 */
public class ManualInspectionRunner {

    @Nullable
    public static IntentionAction inspectElementsForApplicableIntention(@NotNull PsiFile file,
                                                                        @NotNull PsiElement targetElement,
                                                                        int offset,
                                                                        @NotNull ProgressIndicator progress) {
        ProblemDescriptor descriptor = inspectElementWithSpecificInspections(targetElement, offset, progress, getInspectionsForPreview());
        if (descriptor == null) return null;

        @Nullable QuickFix<?>[] fixes = descriptor.getFixes();
        if (fixes != null && fixes.length > 0) {
            return adaptIntention(file, descriptor);
        }
        return null;
    }

    /**
     * Run the Chosen Inspections on the element to get the first matching IntentionAction
     * @return an applicable ProblemDescriptor or null
     */
    @Nullable
    private static ProblemDescriptor inspectElementWithSpecificInspections(@NotNull PsiElement targetElement,
                                                                           int offset,
                                                                           @NotNull ProgressIndicator progress,
                                                                           @NotNull Map<String, LocalInspectionToolWrapper> inspectionsToRun) {
        List<LocalInspectionToolWrapper> toolsToRun = Utils.toList(inspectionsToRun.values());
        
        Map<String, List<ProblemDescriptor>> map;
        try {
            map = inspectElementWithTools(targetElement, offset, progress, toolsToRun);
            if (map.isEmpty()) return null;
        } catch (Exception | Error e) {
            Logger.getInstance(ManualInspectionRunner.class).info("Couldn't run inspections", e);
            return null;
        }

        List<ProblemDescriptor> problemList = getFirst(map.entrySet()).getValue();
        if (!problemList.isEmpty()) {
            return getFirst(problemList);
        }
        return null;
    } // ProblemDescriptor inspectElementWithSpecificInspections(...)

    /**
     * Runs the provided inspections on the element 
     */
    @NotNull
    private static Map<String, List<ProblemDescriptor>> inspectElementWithTools(@NotNull PsiElement targetElement,
                                                                                int offset,
                                                                                @NotNull ProgressIndicator progress,
                                                                                List<LocalInspectionToolWrapper> inspectionTools) throws Exception {
        final List<PsiElement> elements = collectRelevantElements(targetElement, offset);
        final Set<String> dialectIds = InspectionEngine.calcElementDialectIds(elements);

        PsiFile file = targetElement.getContainingFile();
        Project project = file.getProject();
        InspectionManager iManager = InspectionManager.getInstance(project);
        if (!targetElement.isPhysical()) { // Wrap inspection manager to mute an assertion in ProblemDescriptorBase
            iManager = new InspectionManagerOverride(iManager); // which gets thrown when a ProblemDescriptor from
        } // non-physical psielements (from generated sources) is created

        return InspectionEngineBridge.runInspectElements(inspectionTools, file, iManager, true, progress, elements, dialectIds);
    } // Map<String, List<ProblemDescriptor>> inspectElementWithProvidedTools(...)

    /**
     * Converts a problemdescriptor to an IntentionAction by the means of {@link QuickFixWrapper}
     */
    @Nullable
    private static IntentionAction adaptIntention(@NotNull PsiFile psiFile, @NotNull ProblemDescriptor descriptor) {
        if (psiFile instanceof ClsFileImpl) { // Workaround for Decompiled Class files
            return descriptor.getPsiElement() != null ? new ClassFileIntentionActionWrapper(descriptor) : null;
        }
        return InspectionUtils.adaptToIntentionAction(descriptor, psiFile); // Should be StreamToLoopFix inside
    }

    /**
     * Collect elements like in
     * {@link com.intellij.codeInsight.daemon.impl.DoNotShowInspectionIntentionMenuContributor#collectIntentionsFromDoNotShowLeveledInspections(Project, PsiFile, PsiElement, int, ShowIntentionsPass.IntentionsInfo)}
     */
    private static List<PsiElement> collectRelevantElements(@NotNull PsiElement targetElement, int offset) {
        PsiUtilCore.ensureValid(targetElement);
        final PsiElement psiElement = targetElement;

        List<PsiElement> elements = PsiTreeUtil.collectParents(targetElement, PsiElement.class, true, e -> e instanceof PsiDirectory);

        PsiElement elementToTheLeft = psiElement.getContainingFile().findElementAt(offset - 1);
        if (elementToTheLeft != psiElement && elementToTheLeft != null) {
            List<PsiElement> parentsOnTheLeft =
                    PsiTreeUtil.collectParents(elementToTheLeft, PsiElement.class, true, e -> e instanceof PsiDirectory || elements.contains(e));
            elements.addAll(parentsOnTheLeft);
        }

        return elements;
    } // List<PsiElement> collectRelevantElements(...)

    private static Map<String, LocalInspectionToolWrapper> getInspectionsForPreview() {
        return InspectionsHolder.myInspections;
    }

    public static class InspectionsHolder {
        @Required
        @NotNull
        public static final Map<String, LocalInspectionToolWrapper> myInspections = createInspectionsMap();

        @NotNull
        private static Map<String, LocalInspectionToolWrapper> createInspectionsMap() {
            try {
                return inspections().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, inspection -> new LocalInspectionToolWrapper(inspection.getValue()), (a, b) -> b));
            } catch (Exception e) {
                StreamTipsPluginDiagnostics.recordPluginInitException(e);
                return Collections.emptyMap();
            }
        }
    }

    private static Map<String, LocalInspectionTool> inspections() {
        return Collections.singletonMap("StreamToLoop", new StreamToLoopInspection());
    }

} // class ManualInspectionRunner
