package me.foreverigor.intellij.plugin.streamtips;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorMouseHoverPopupControl;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PopupUtils {

  static boolean isPopupDisabled(Editor editor) {
    return isAnotherAppInFocus() ||
           EditorMouseHoverPopupControl.arePopupsDisabled(editor) ||
           LookupManager.getActiveLookup(editor) != null ||
           isAnotherPopupFocused() ||
           isContextMenuShown();
  }

  private static boolean isAnotherAppInFocus() {
    return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null;
  }

  // e.g. if documentation popup (opened via keyboard shortcut) is already shown
  private static boolean isAnotherPopupFocused() {
    JBPopup popup = PopupUtil.getPopupContainerFor(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
    return popup != null && !popup.isDisposed();
  }

  private static boolean isContextMenuShown() {
    return MenuSelectionManager.defaultManager().getSelectedPath().length > 0;
  }

  static Rectangle getPopupBounds(@NotNull Editor editor, @NotNull JBPopup popup) {
    Dimension size = popup.getSize();
    if (size == null) return null;
    Rectangle result = new Rectangle(popup.getLocationOnScreen(), size);
    int borderTolerance = editor.getLineHeight() / 3;
    result.grow(borderTolerance, borderTolerance);
    return result;
  }

  private PopupUtils() {}

}
