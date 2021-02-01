// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Modifications by foreverigor
package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.IdeEventQueue;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.reference.SoftReference;
import com.intellij.ui.MouseMovementTracker;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;

import me.foreverigor.intellij.platform.annotations.InvokedLater;
import me.foreverigor.intellij.platform.annotations.OnDispatchThread;
import me.foreverigor.intellij.plugin.streamtips.inspect.ManualInspectionRunner;
import static me.foreverigor.intellij.plugin.streamtips.PopupUtils.isPopupDisabled;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Adapted from {@link com.intellij.openapi.editor.EditorMouseHoverPopupManager}
 */
@Service
public final class MouseHoverIntentPreviewPopupService implements Disposable {

  // Concurrency:
  private final Alarm myAlarm;
  private ProgressIndicator myCurrentProgress;
  private CancellablePromise<?> myPreparationTask;

  private final MouseMovementTracker myMouseMovementTracker = new MouseMovementTracker();
  private WeakReference<Editor> myCurrentEditor;
  private WeakReference<AbstractPopup> myPopupReference;
  private PopupTipContext myContext; // TODO comparison of contexts
  private boolean skipNextEvent;

  public MouseHoverIntentPreviewPopupService() {
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    multicaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent event) {
        Editor editor = event.getEditor();
        if (editor == SoftReference.dereference(myCurrentEditor)) {
          DocumentationManager.getInstance(Objects.requireNonNull(editor.getProject())).setAllowContentUpdateFromContext(true);
        }
      }
    }, this);
    multicaster.addVisibleAreaListener(e -> {
      Rectangle oldRectangle = e.getOldRectangle();
      if (e.getEditor() == SoftReference.dereference(myCurrentEditor) &&
          oldRectangle != null && !oldRectangle.getLocation().equals(e.getNewRectangle().getLocation())) {
        cancelAndClosePopup();
      }
    }, this);

    EditorMouseHoverPopupControl.getInstance().addListener(() -> {
      Editor editor = SoftReference.dereference(myCurrentEditor);
      if (editor != null && EditorMouseHoverPopupControl.arePopupsDisabled(editor)) {
        closePopup();
      }
    });
    LaterInvocator.addModalityStateListener(new ModalityStateListener() {
      @Override
      public void beforeModalityStateChanged(boolean entering, @NotNull Object modalEntity) {
        cancelAndClosePopup();
      }
    }, this);
    IdeEventQueue.getInstance().addDispatcher(event -> {
      int eventID = event.getID();
      if (eventID == KeyEvent.KEY_PRESSED || eventID == KeyEvent.KEY_TYPED) {
        cancelAndClosePopup(); // Close on everything
      }
      return false;
    }, this);
    //ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, new EditorTypingListener());
  }

  @Override
  public void dispose() {}

  private static boolean earlyCalculate() {
    return StreamTipsPluginUtils.getShouldEarlyCalculate();
  }

  private static boolean chainPopupLoading() {
    return StreamTipsPluginUtils.getDrawPopupOnPreviewAvailable();
  }

  private void handleMouseMoved(@NotNull EditorMouseEvent e) {
    long startTimestamp = System.currentTimeMillis();

    cancelCurrentProcessing();

    if (ignoreEvent(e)) return;

    Editor editor = e.getEditor();
    if (isPopupDisabled(editor)) {
      closePopup();
      return;
    }

    int targetOffset = getTargetOffset(e);
    if (targetOffset < 0) {
      closePopup();
      return;
    }
    PsiFile sourceFile;
    if ((sourceFile = getPsiFileIfApplicable(editor)) == null) {
      return;
    }

    NonBlockingReadAction<?> action;
    if (earlyCalculate()) {
      ProgressIndicator progressAll = new ProgressIndicatorBase(); // we tear apart the loading stages in this variant of loading,
      // so this outer progress is our way to glue them back together
      action = createPrepTask(() -> calculatePopup(editor, sourceFile, targetOffset, startTimestamp, progressAll), result -> {
        // All on dispatch thread:
        if (preparationCleanup(result, editor)) return;
        // unlike createContext(), calculatePopup() takes time, so check if cancelled here too when we are back on ui:
        if (canceled(progressAll)) return;

        if (chainPopupLoading()) {
          createPopupStartLoading(editor, result.first, result.second, progressAll);
        } else {
          schedulePopupCreation(editor, result.first, result.second, progressAll);
        }
      });
    } else {
      action = createPrepTask(() -> createContext(editor, sourceFile, targetOffset, startTimestamp), context -> {
        myPreparationTask = null;
        if (context == null || !editor.getContentComponent().isShowing()) {
          closePopup();
          return;
        }
        schedulePopupCalculation(editor, context);
      });
    }

    myPreparationTask = action
      .coalesceBy(this)
      .withDocumentsCommitted(Objects.requireNonNull(editor.getProject()))
      .expireWhen(() -> editor.isDisposed())
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private static <T> NonBlockingReadAction<T> createPrepTask(Callable<T> calculation, @OnDispatchThread Consumer<T> calculationConsumer) {
    return ReadAction.nonBlocking(calculation).finishOnUiThread(ModalityState.any(), calculationConsumer);
  }

  /**
   * @return true if should cancel
   */
  @OnDispatchThread
  private <T> boolean preparationCleanup(T prepResult, Editor editor) {
    myPreparationTask = null;
    if (prepResult == null || !editor.getContentComponent().isShowing()) {
      closePopup();
      return true;
    }
    return false;
  }

  @Nullable
  @InvokedLater
  private Pair<PopupTipContext, IntentionAction> calculatePopup(@NotNull Editor editor, @NotNull PsiFile file, int offset, long startTimestamp, ProgressIndicator progress) {
    // When running calculations right away getting psi, context creation and quickFix calc can be chained together like
    // this (everything runs in read action):
    PopupTipContext context = createContext(editor, file, offset, startTimestamp);
    if (context == null) return null;
    myCurrentProgress = progress; // This is where the calculation task sort of actually begins
    final IntentionAction action = ProgressManager.getInstance().runProcess(() -> {
      return context.calculateQuickFixForPopup(myCurrentProgress);
    }, myCurrentProgress); // Unclear if this is slower than executeUnderProgress
    return action != null ? Pair.create(context, action) : null;
  }

  private void schedulePopupCreation(@NotNull Editor editor, @NotNull PopupTipContext context, @NotNull IntentionAction action, ProgressIndicator progress) {
    myAlarm.addRequest(() -> {
      ApplicationManager.getApplication().invokeLater(() -> {
        createAndShowPopup(editor, context, action, progress);
      });
    }, context.getShowingDelay());
  }

  /**
   * Entrypoint for new popup creation/loading logic. This not only pushes the inspection calculation to an earlier
   * point like {@link #schedulePopupCreation(Editor, PopupTipContext, IntentionAction, ProgressIndicator)} does,
   * it also chains the start of popup loading to it, making all the calculations execute as soon as possible.
   * As the popup loading involves a lengthy calculation of the preview, this is the only way how to hide the loading
   * process (drawn in the popup) from the user and honor a very low delay setting (only to a degree).
   *
   * <p> How this works: kicking off the popup loading normally leads to it being drawn with the "loading" text, this is
   * what {@link #showPopup(IntentionPreviewPopup, Editor, PopupTipContext)} does. When performing early popup loading
   * we want to prevent this (or else the popup would always appear before the specified delay). So, new logic in
   * IntentionPreviewPopup let's us intercept the popup starting to load and prevent it from showing immediately and,
   * ideally, get a callback of exactly when the preview is finished so we can schedule showing the popup.
   */
  @OnDispatchThread
  private void createPopupStartLoading(@NotNull Editor editor,
                                       @NotNull PopupTipContext context,
                                       @NotNull IntentionAction action,
                                       ProgressIndicator originalProgress) {
    IntentionPreviewPopup intentPopup = createPopup(editor, context, action);
    if (intentPopup != null) {
      intentPopup.startLoading((immediateShow, popup) -> onPopupLoadingFinished(immediateShow, popup, editor, context, originalProgress));
    }
  }

  /**
   * Gets called from IntentionPreviewPopup when popup has finished loading (usually), progress has to be checked (!)
   * @param showPopupImmediately if set, draw the popup immediately (this is when setting the listener failed)
   */
  @InvokedLater
  private void onPopupLoadingFinished(boolean showPopupImmediately,
                                      @NotNull AbstractPopup popup,
                                      @NotNull Editor editor,
                                      @NotNull PopupTipContext context,
                                      ProgressIndicator progress) {
    if (canceled(progress)) return;

    if (showPopupImmediately || context.getShowingDelay() == 0) {
      showPopupAndSet(popup, editor, context); // We are on dispatch and can show immediately
    } else {
      schedulePopupDisplay(popup, editor, context, progress);
    }
  }

  private void schedulePopupDisplay(@NotNull AbstractPopup popup, @NotNull Editor editor, @NotNull PopupTipContext context, ProgressIndicator progress) {
    myAlarm.cancelAllRequests(); // if there other requests we scheduled, cancel them
    scheduleWithContextDelay((@InvokedLater Runnable)() -> ApplicationManager.getApplication().invokeLater(() -> {
      if (canceled(progress)) return; // Important to check here if canceled
      showPopupAndSet(popup, editor, context);
    }), context);
  }

  @Nullable
  private IntentionPreviewPopup createPopup(@NotNull Editor editor,
                                            @NotNull PopupTipContext context,
                                            @NotNull IntentionAction action) {
    if (editor.getContentComponent().isShowing() && !isPopupDisabled(editor)) {
      IntentionPreviewPopup fixPopup = IntentionPreviewPopup.createPopup(editor, context.getFileForInspection(), action);
      if (fixPopup == null) closePopup();
      return fixPopup;
    }
    return null;
  }

  @OnDispatchThread
  @InvokedLater
  private void createAndShowPopup(@NotNull Editor editor,
                                  @NotNull PopupTipContext context,
                                  @NotNull IntentionAction action,
                                  ProgressIndicator originalProgress) {
    if (originalProgress == myCurrentProgress) {
      myCurrentProgress = null;
      IntentionPreviewPopup popup = createPopup(editor, context, action);
      if (popup != null) {
        showPopupAndSet(popup, editor, context);
      }
    }
  }

  @OnDispatchThread
  private void showPopupAndSet(IntentionPreviewPopup intentPopup, @NotNull Editor editor, @NotNull PopupTipContext context) {
    showPopupAndSet(() -> showPopup(intentPopup, editor, context), editor, context);
  }

  @OnDispatchThread
  private void showPopupAndSet(AbstractPopup popup, @NotNull Editor editor, @NotNull PopupTipContext context) {
    showPopupAndSet(() -> showPopup(popup, editor, context), editor, context);
  }

  /**
   * Executes the showPopup action and then sets the resulting popup as current
   */
  @OnDispatchThread
  private void showPopupAndSet(Computable<AbstractPopup> showPopup, @NotNull Editor editor, @NotNull PopupTipContext context) {
    AbstractPopup popup = showPopup.get();
    if (popup != null) {
      myPopupReference = new WeakReference<>(popup);
      myCurrentEditor = new WeakReference<>(editor);
      myContext = context;
    }
  }

  private void cancelCurrentProcessing() {
    if (myPreparationTask != null) {
      myPreparationTask.cancel();
      myPreparationTask = null;
    }
    myAlarm.cancelAllRequests();
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
      myCurrentProgress = null;
    }
  }

  private boolean canceled(ProgressIndicator progress) {
    return progress != myCurrentProgress;
  }

  private void skipNextMovement() {
    skipNextEvent = true;
  }

  private void schedulePopupCalculation(@NotNull Editor editor,
                                        @NotNull PopupTipContext context) {
    ProgressIndicatorBase progress = new ProgressIndicatorBase();
    myCurrentProgress = progress;
    myAlarm.addRequest(() -> {
      ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        Application application = ApplicationManager.getApplication();
        final IntentionAction fixAction = application.runReadAction((Computable<IntentionAction>) () -> context.calculateQuickFixForPopup(progress));
        if (fixAction == null) return;
        application.invokeLater(() -> {
          // All dispatch thread
          if (canceled(progress)) return;

          // After calculations:
          myCurrentProgress = null;
          if (!editor.getContentComponent().isShowing() || isPopupDisabled(editor)) {
            return;
          }

          createAndShowPopup(editor, context, fixAction, myCurrentProgress);
        });
      }, progress);
    }, context.getShowingDelay());
  }

  private void scheduleUnderProgress(Runnable runnable, ProgressIndicator progress, @NotNull PopupTipContext context) {
    scheduleWithContextDelay(() -> underProgress(runnable, progress), context);
  }

  private void scheduleWithContextDelay(@InvokedLater Runnable runnable, @NotNull PopupTipContext context) {
    myAlarm.addRequest(runnable, context.getShowingDelay());
  }

  private static void underProgress(Runnable runnable, ProgressIndicator progress) {
    ProgressManager.getInstance().executeProcessUnderProgress(runnable, progress);
  }

  private boolean ignoreEvent(EditorMouseEvent e) {
    if (skipNextEvent) {
      skipNextEvent = false;
      return true;
    }
    Rectangle currentHintBounds = getCurrentPopupBounds(e.getEditor()); // don't keep popup on mouse move in general
    return myMouseMovementTracker.isMovingTowards(e.getMouseEvent(), currentHintBounds);
  }

  private Rectangle getCurrentPopupBounds(Editor editor) {
    JBPopup popup = getCurrentPopup();
    if (popup == null) return null;
    return PopupUtils.getPopupBounds(editor, popup);
  }

  @Nullable
  @OnDispatchThread
  private AbstractPopup showPopup(@NotNull IntentionPreviewPopup intentPopup, @NotNull Editor editor, PopupTipContext context) {
    return showPopupInEditor(intentPopup::show, editor, context);
  }

  /**
   * Directly show the supplied AbstractPopup
   */
  @Nullable
  private AbstractPopup showPopup(@NotNull AbstractPopup popup, @NotNull Editor editor, PopupTipContext context) {
    return showPopupInEditor(() -> {
      if (popup.isDisposed()) return null; // In some cases the popup may already be disposed when we arrive here
      popup.showInBestPositionFor(editor);
      return popup;
    }, editor, context);
  }

  /**
   * @param popupShow computable, call to which shows the popup
   */
  @Nullable
  @OnDispatchThread
  private AbstractPopup showPopupInEditor(@OnDispatchThread Computable<AbstractPopup> popupShow,
                                          @NotNull Editor editor,
                                          PopupTipContext context) {
    closePopup();
    myMouseMovementTracker.reset();
    editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, context.getPopupPosition(editor)); // Our popup honors this
    AbstractPopup popup;
    try {
      popup = popupShow.get();
      if (popup == null) return null; // Something went wrong
    }
    finally {
      editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
    }
    Window window = popup.getPopupWindow();
    if (window != null) {
      //window.setFocusableWindowState(true); // Don't focus(!)
      IdeEventQueue.getInstance().addDispatcher(e -> {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getSource() == window) {
          //myKeepPopupOnMouseMove = true; // This was made so that you can click inside the hint, not needed for now
        }
        else if (e.getID() == WindowEvent.WINDOW_OPENED && !isParentWindow(window, e.getSource())) {
          closePopup();
        }
        return false;
      }, popup);
    }
    return popup;
  }

  private static boolean isParentWindow(@NotNull Window parent, Object potentialChild) {
    return parent == potentialChild ||
           (potentialChild instanceof Component) && isParentWindow(parent, ((Component)potentialChild).getParent());
  }

  private static int getTargetOffset(EditorMouseEvent event) {
    Editor editor = event.getEditor();
    if (editor instanceof EditorEx &&
        editor.getProject() != null &&
        event.getArea() == EditorMouseEventArea.EDITING_AREA &&
        event.getMouseEvent().getModifiers() == 0 &&
        event.isOverText() &&
        event.getCollapsedFoldRegion() == null) {
      return event.getOffset();
    }
    return -1;
  }

  @Nullable
  private static PopupTipContext createContext(@NotNull Editor editor, @NotNull PsiFile file, int offset, long startTimestamp) {
    Project project = Objects.requireNonNull(editor.getProject());

    HighlightInfo info = null;
    if (!Registry.is("ide.disable.editor.tooltips")) {
      DaemonCodeAnalyzerImpl daemonCodeAnalyzer = (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
      boolean highestPriorityOnly = !Registry.is("ide.tooltip.showAllSeverities");
      info = daemonCodeAnalyzer
        .findHighlightsByOffset(editor.getDocument(), offset, false, highestPriorityOnly, HighlightSeverity.INFORMATION);
    }
    Pair<Integer, PsiElement> elementAndOffset = getPsiElementAtOffset(file, offset);
    if (elementAndOffset == null) return null;
    return new PopupTipContext(startTimestamp, info, file, elementAndOffset.first, elementAndOffset.second);
  }

  /**
   * Adapted from
   * {@link com.intellij.codeInsight.daemon.impl.DoNotShowInspectionIntentionMenuContributor#collectActions(Editor, PsiFile, ShowIntentionsPass.IntentionsInfo, int, int)}
   */
  @Nullable
  private static Pair<Integer, PsiElement> getPsiElementAtOffset(@NotNull PsiFile psiFile, int offset) {
    final PsiElement psiElement = psiFile.findElementAt(offset);
    if (psiElement == null) {
      return null;
    }
    int intentionOffset = offset;
    PsiElement intentionElement = psiElement;
    if (psiElement instanceof PsiWhiteSpace && offset == psiElement.getTextRange().getStartOffset() && offset > 0) {
      final PsiElement prev = psiFile.findElementAt(offset - 1);
      if (prev != null && prev.isValid()) {
        intentionElement = prev;
        intentionOffset = offset - 1;
      }
    }
    return Pair.create(intentionOffset, intentionElement);
  } // Pair<Integer, PsiElement> getPsiElementAtOffset(@NotNull PsiFile psiFile, int offset)

  private void cancelAndClosePopup() {
    cancelCurrentProcessing();
    closePopup();
  }

  private void closePopup() {
    AbstractPopup popup = getCurrentPopup();
    if (popup != null) {
      popup.cancel();
    }
    myPopupReference = null;
    myCurrentEditor = null;
    myContext = null;
  }

  private boolean isPopupShown() {
    return getCurrentPopup() != null;
  }

  private AbstractPopup getCurrentPopup() {
    if (myPopupReference == null) return null;
    AbstractPopup popup = myPopupReference.get();
    if (popup == null || !popup.isVisible()) {
      if (popup != null) {
        // popup's window might've been hidden by AWT without notifying us
        // dispose to remove the popup from IDE hierarchy and avoid leaking components
        popup.cancel();
      }
      myPopupReference = null;
      myCurrentEditor = null;
      myContext = null;
      return null;
    }
    return popup;
  }

  private static class PopupTipContext {
    private final long startTimestamp;
    private final int targetOffset;
    private final WeakReference<HighlightInfo> highlightInfo;
    private final WeakReference<PsiFile> psiFile;
    private final WeakReference<PsiElement> targetElement;

    private PopupTipContext(long startTimestamp,
                            @Nullable HighlightInfo highlightInfo,
                            @NotNull PsiFile file,
                            int targetOffset,
                            @NotNull PsiElement elementForPopup) {
      this.startTimestamp = startTimestamp;
      this.targetOffset = targetOffset;
      this.highlightInfo = highlightInfo == null ? null : new WeakReference<>(highlightInfo);
      this.targetElement = new WeakReference<>(elementForPopup);
      this.psiFile = new WeakReference<>(file);
    }

    @Nullable
    private IntentionAction calculateQuickFixForPopup(ProgressIndicator progress) {
      PsiElement element = getElementForInspection();
      PsiFile file = getFileForInspection();
      if (element == null || file == null) return null;
      return ManualInspectionRunner.inspectElementsForApplicableIntention(file, element, targetOffset, progress);
    }

    private PsiElement getElementForInspection() {
      return SoftReference.dereference(targetElement);
    }

    private PsiFile getFileForInspection() {
      return SoftReference.dereference(psiFile);
    }

    private HighlightInfo getHighlightInfo() {
      return SoftReference.dereference(highlightInfo);
    }

    private Relation compareTo(PopupTipContext other) {
      if (other == null) return Relation.DIFFERENT;
      HighlightInfo highlightInfo = getHighlightInfo();
      if (!Objects.equals(highlightInfo, other.getHighlightInfo())) return Relation.DIFFERENT;
      return Objects.equals(getElementForInspection(), other.getElementForInspection())
             ? Relation.SAME
             : highlightInfo == null ? Relation.DIFFERENT : Relation.SIMILAR;
    }

    long getShowingDelay() {
      return Math.max(0, StreamTipsPluginUtils.getPopuptipDelay() - (System.currentTimeMillis() - startTimestamp));
    }

    private static int getElementStartHostOffset(@NotNull PsiElement element) {
      int offset = element.getTextRange().getStartOffset();
      Project project = element.getProject();
      PsiFile containingFile = element.getContainingFile();
      if (containingFile != null && InjectedLanguageManager.getInstance(project).isInjectedFragment(containingFile)) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document instanceof DocumentWindow) {
          return ((DocumentWindow)document).injectedToHost(offset);
        }
      }
      return offset;
    }

    @NotNull
    @OnDispatchThread
    private VisualPosition getPopupPosition(Editor editor) {
      HighlightInfo highlightInfo = getHighlightInfo();
      if (highlightInfo == null) {
        int offset = targetOffset;
        PsiElement elementForInspection = getElementForInspection();
        if (elementForInspection != null && elementForInspection.isValid()) {
          offset = getElementStartHostOffset(elementForInspection);
        }
        return editor.offsetToVisualPosition(offset);
      }
      else {
        VisualPosition targetPosition = editor.offsetToVisualPosition(targetOffset);
        VisualPosition endPosition = editor.offsetToVisualPosition(highlightInfo.getEndOffset());
        if (endPosition.line <= targetPosition.line) return targetPosition;
        Point targetPoint = editor.visualPositionToXY(targetPosition);
        Point endPoint = editor.visualPositionToXY(endPosition);
        Point resultPoint = new Point(targetPoint.x, endPoint.x > targetPoint.x ? endPoint.y : editor.visualLineToY(endPosition.line - 1));
        return editor.xyToVisualPosition(resultPoint);
      }
    }

    private enum Relation {
      SAME, // no need to update popup
      SIMILAR, // popup needs to be updated
      DIFFERENT // popup needs to be closed, and new one shown
    }
  } // class PopupTipContext

  private PsiFile getPsiFileIfApplicable(@NotNull Editor editor) {
    try {
      Document editorDocument = editor.getDocument();
      PsiFile psiFile = PsiDocumentManager.getInstance(Objects.requireNonNull(editor.getProject())).getPsiFile(editorDocument);
      if (psiFile instanceof PsiJavaFile) {
        if (((PsiJavaFile) psiFile).getLanguageLevel().isAtLeast(LanguageLevel.JDK_1_8))
          return psiFile;
      }
    } catch (Exception e) {}
    return null;
  } // PsiFile getPsiFileIfApplicable(@NotNull Editor editor)

  @NotNull
  public static MouseHoverIntentPreviewPopupService getInstance() {
    return ApplicationManager.getApplication().getService(MouseHoverIntentPreviewPopupService.class);
  }

  static final class MyEditorMouseMotionEventListener implements EditorMouseMotionListener {
    @Override
    public void mouseMoved(@NotNull EditorMouseEvent e) {
      getInstance().handleMouseMoved(e);
    }
  }

  static final class MyEditorMouseEventListener implements EditorMouseListener {
    @Override
    public void mouseEntered(@NotNull EditorMouseEvent event) {
      // we receive MOUSE_MOVED event after MOUSE_ENTERED even if mouse wasn't physically moved,
      // e.g. if a popup overlapping editor has been closed
      getInstance().skipNextMovement();
    }

    @Override
    public void mouseExited(@NotNull EditorMouseEvent event) {
      getInstance().cancelCurrentProcessing();
    }

    @Override
    public void mousePressed(@NotNull EditorMouseEvent event) {
      getInstance().cancelAndClosePopup();
    }
  }

  /**
   * Don't listen for beforeActionPerformed here, closing the popup here leads to an exception in a different call of it where
   * {@link com.intellij.openapi.editor.impl.InspectionPopupManager#hidePopup()} gets called.
   * beforeEditorTyping also not needed because we close on every keypress
   */
  private static class EditorTypingListener implements AnActionListener {
    @Override
    public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
      getInstance().cancelAndClosePopup();
    }
  }
} // class MouseHoverIntentPreviewPopupService
