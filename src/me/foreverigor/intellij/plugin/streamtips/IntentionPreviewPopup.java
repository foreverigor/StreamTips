package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewPopupUpdateProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBLoadingPanelListener;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.util.ConcurrencyUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.foreverigor.intellij.platform.annotations.OnDispatchThread;
import me.foreverigor.intellij.plugin.streamtips.inspect.overrides.ClassFileIntentionActionWrapper;
import me.foreverigor.utils.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Nullable
    @OnDispatchThread
    AbstractPopup show() {
        return startLoading(null); // Will show popup loading when called
    }

    /**
     * @param showPopupCallback if not null, intercepts initial popup showing. Gets called when the popup finishes
     *                          loading, boolean parameter represents the need for the popup to be shown immediately
     */
    @Nullable
    AbstractPopup startLoading(@Nullable BiConsumer<Boolean, AbstractPopup> showPopupCallback) {
        try {
            popupProcessor.toggleShow(); // TODO show isn't false by default, depends on experiments setting
            return interceptPopup(supplier -> {
                popupProcessor.setup(s -> (Unit) supplier.get(),0);
                popupProcessor.updatePopup(myAction);
            }, showPopupCallback);
        } catch (Exception e) {
            Logger.getInstance(IntentionPreviewPopup.class).error("Popup error", e);
        }
        return null;
    } // AbstractPopup startLoading(@Nullable BiConsumer<Boolean, AbstractPopup> showPopupCallback)

    /**
     * There are 3 ways of intercepting the popup creation:
     * <p> 1. After {@link IntentionPreviewPopupUpdateProcessor#updatePopup(Object)} returns, the popup has been created
     * and written to the field, but it has already been shown and started loading.
     * <p> 2. updatePopup() calls startInWriteAction() and getElementToMakeWritable() on the IntentionAction passed to
     * it before it calls .startLoading() on the popup component. We can wrap the action and intercept those calls and
     * be sure that the popup field in the processor has been set, but again, at this point the popup is already shown
     * in the editor
     * <p> 2b. it also calls the updateAdvertiserText function (Consumer) which we have to set anyway right after showing
     * the popup, same restriction as with 2. apply but this is better
     * <p> 3. Some components, including AbstractPopup, call {@link com.intellij.ui.UiInterceptors#tryIntercept(Object)}
     * before showing, and if an interceptor is available, the component will not be shown. The interceptor even gets
     * the component instance, which is the popup in our case – just what we need.
     * {@link UiInterceptors#register(UiInterceptors.UiInterceptor)} is marked as TestOnly which I'm a little unsure
     * about but this is the only way how to actually intercept the popup showing and hide the loading step without reflection trickery
     *
     * @param popupSetup popupProcessor setup() and updatePopup() calls, takes the function to be passed to setup() for
     *                   intercepting
     * @param showPopupCallback callback which gets called when the popup loading has finished and preview should be
     *                           avialable
     */
    @Nullable
    private AbstractPopup interceptPopup(Consumer<Supplier<?>> popupSetup, BiConsumer<Boolean, AbstractPopup> showPopupCallback) {
        final Ref<JBPopup> popupRef = new Ref<>();

        if (showPopupCallback != null) { // intercept popup showing, try to don't show and execute this when the preview will be available
            UiInterceptors.register(new UiInterceptors.UiInterceptor<>(Object.class) { // Interceptor can throw an exception
                // if it gets called from a different place, which we don't want, so set class Object just in case
                @Override
                protected void doIntercept(@NotNull Object component) {
                    UiInterceptors.clear();
                    if (component instanceof AbstractPopup) { // TODO also check if this is really our popup (check call stack)
                        popupRef.set((AbstractPopup)component);
                        if (!addPopupReadyInterceptor((AbstractPopup) component, popup -> showPopupCallback.accept(false, popup))) {
                            // Pretty bad, we couldn't set the loadingFinish listener so we will not receive a
                            // callback for scheduling showPopup, instead show popup immediately:
                            showPopupCallback.accept(true, (AbstractPopup) component);
                            // This way we essentially fall back to the old behaviour (popup will show loading step)
                        }
                    } else { // We intercepted (and prevented from showing) something different, disable this behaviour:
                        StreamTipsPluginUtils.setOverrideDisableChainLoading();
                    }
                }
            });
        }
        // 2nd variant which we do when popup loading isn't hidden and in case 3rd won't work
        // (in this case we will not be able to intercept showing the popup)
        popupSetup.accept(() -> {
            if (popupRef.isNull()) popupRef.set(extractPopup(popupProcessor));
            return null;
        }); // After this returns, we should've extracted the popup field
        UiInterceptors.clear(); // paranoia
        JBPopup popup = popupRef.get();
        return popup instanceof AbstractPopup ? (AbstractPopup) popup : null;
    } // AbstractPopup interceptPopup(Consumer<Supplier<?>> popupSetup, BiConsumer<Boolean, AbstractPopup> showPopupCallback)

    /**
     * This is done here and not after returning to the Service because updatePopup() is already done executing in this
     * case and we might be too late, want to start listening as soon as possible - even before the calculation is
     * started is ideal
     */
    private static boolean addPopupReadyInterceptor(AbstractPopup popup, Consumer<AbstractPopup> onPopupLoadingFinish) {
        try {
            ((JBLoadingPanel) popup.getComponent()).addListener(new PopupLoadingListener(popup, onPopupLoadingFinish));
            return true;
        } catch (Exception e) {
            Logger.getInstance(IntentionPreviewPopup.class).error("Popup error", e);
        }
        StreamTipsPluginUtils.setOverrideDisableChainLoading();
        return false;
    } // boolean addPopupReadyInterceptor(AbstractPopup popup, Consumer<AbstractPopup> onPopupLoadingFinish)

    /**
     * TODO somehow check that the index in {@link IntentionPreviewPopupUpdateProcessor#select(int, List)} isn't NO_PREVIEW
     * Maybe add a finished listener in a different place ?
     * TODO when the preview has an import statement, on the first showing only it will be shown, our code interferes somehow
     */
    private static class PopupLoadingListener implements JBLoadingPanelListener {
        private final Runnable onPopupLoadingFinish;

        public PopupLoadingListener(AbstractPopup popup, Consumer<AbstractPopup> loadedPopupConsumer) {
            this.onPopupLoadingFinish = ConcurrencyUtil.once(() -> loadedPopupConsumer.accept(popup));
        }
        @Override
        public void onLoadingFinish() {
            try {
                onPopupLoadingFinish.run();
            } catch (Exception e) {
                Logger.getInstance(IntentionPreviewPopup.class).error("Popup error", e);
                // Exception would be thrown up and outside of the plugin
            }
        }
        @Override
        public void onLoadingStart() {}
    } // class PopupLoadingListener

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
                return null; // Don't even attempt to show the popup if this fails
            }
        }
        // These get checked in the processor, might as well check them here:
        if (!actionToShowFor.startInWriteAction()) {
            return null;
        }
        PsiElement element = actionToShowFor.getElementToMakeWritable(sourceFile);
        if (element == null || element.getContainingFile() != sourceFile) {
            return null;
        }
        return new IntentionPreviewPopup(project, sourceFile, editor, actionToShowFor);
    }

    static class PopupFieldExtractorHolder {
        static final Function<IntentionPreviewPopupUpdateProcessor, JBPopup> popupFieldGetter = createPopupGetter();

        private static Function<IntentionPreviewPopupUpdateProcessor, JBPopup> createPopupGetter() {
            try {
                return ReflectionUtils.getGetterForFieldExact(IntentionPreviewPopupUpdateProcessor.class, "popup", JBPopup.class,
                        new ReflectionUtils.ExactInvocator<>() {
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
