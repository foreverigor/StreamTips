package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewPopupUpdateProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.popup.AbstractPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.foreverigor.intellij.plugin.streamtips.inspect.overrides.ClassFileIntentionActionWrapper;
import me.foreverigor.utils.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.util.function.Function;

class IntentionPreviewPopup {

    private final IntentionPreviewPopupUpdateProcessor popupProcessor;
    private final IntentionAction myAction;

    IntentionPreviewPopup(@NotNull IntentionPreviewPopupUpdateProcessor popupProcessor, @NotNull IntentionAction action) {
        this.popupProcessor = popupProcessor;
        this.myAction = action;
    }

    IntentionPreviewPopup(@NotNull Project project, @NotNull PsiFile file, @NotNull Editor editor, @NotNull IntentionAction action) {
        this(new IntentionPreviewPopupUpdateProcessor(project, file, editor), action);
    }

    AbstractPopup show() {
        popupProcessor.setup(s -> null, 0);
        popupProcessor.toggleShow();
        popupProcessor.updatePopup(myAction); // After this returns, the popup field in the processor has been sent, so we can get it
        JBPopup popup = extractPopup(popupProcessor);
        return popup instanceof AbstractPopup ? (AbstractPopup) popup : null; // If null, something went wrong
    }

    private static JBPopup extractPopup(IntentionPreviewPopupUpdateProcessor processor) {
        return PopupFieldExtractorHolder.popupFieldGetter.apply(processor);
    }

    @Nullable
    static IntentionPreviewPopup createPopup(@NotNull Editor editor, @Nullable PsiFile file, @NotNull IntentionAction actionToShowFor) {
        Project project = editor.getProject();
        if (file == null || project == null) return null;
        PsiFile sourceFile = file;
        // Workaround for decompiled/ attached source files: (feed the psiFile for the generated/attached soure file into the preview)
        if (actionToShowFor instanceof ClassFileIntentionActionWrapper) {
            PsiElement element = ((ClassFileIntentionActionWrapper) actionToShowFor).getDescriptor().getPsiElement();
            if (element == null || (sourceFile = element.getContainingFile()) == null) {
                return null;
            }
        }
        return new IntentionPreviewPopup(project, sourceFile, editor, actionToShowFor);
    }

    static class PopupFieldExtractorHolder {
        static final Function<IntentionPreviewPopupUpdateProcessor, JBPopup> popupFieldGetter = createPopupGetter();

        private static Function<IntentionPreviewPopupUpdateProcessor, JBPopup> createPopupGetter() {
            try {
                return ReflectionUtils.getGetterForFieldExact(IntentionPreviewPopupUpdateProcessor.class, "popup", JBPopup.class,
                        new ReflectionUtils.ExactInvocator<JBPopup, IntentionPreviewPopupUpdateProcessor>() {
                            @Override
                            public JBPopup invokeExactInvoke(MethodHandle handleToInvoke, IntentionPreviewPopupUpdateProcessor o) throws Throwable {
                                return (JBPopup) handleToInvoke.invokeExact(o);
                            }
                        });
            } catch (Exception e) {}
            return null;
        }
    } // class PopupFieldExtractorHolder

} // class IntentionPreviewPopup
