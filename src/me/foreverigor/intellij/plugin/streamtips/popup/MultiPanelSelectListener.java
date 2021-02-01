package me.foreverigor.intellij.plugin.streamtips.popup;

import com.intellij.ide.plugins.MultiPanel;
import com.intellij.ui.CardLayoutPanel;
import org.jetbrains.annotations.NotNull;

import me.foreverigor.utils.FunctionBiConsumer;
import me.foreverigor.utils.ReflectionUtils;

import javax.swing.*;
import java.lang.invoke.MethodHandle;
import java.util.IdentityHashMap;

/**
 * Explanation: We want to intercept a call to select() in the popupProcessor and get the selection index. select() in
 * the processor calls {@link MultiPanel#select(Object, boolean)} on MultiPanel, the instance of which is stored
 * in the components of the content panel of the popup component, so we have access to it. I don't want to override and
 * replace any of the components, so instead I get and replace the contentMap of the {@link CardLayoutPanel} which gets
 * queried with the relevant index. Another way would be to directly add an empty component at the no_preview index to
 * the multipanel (it's setVisible() would be called with true for in the no_preview case) but that would still mean
 * adding a value to the map.
 */
public class MultiPanelSelectListener {

  public static void installSelectListener(@NotNull MultiPanel multiPanel, int selectKey, Runnable cancelPopup) throws Exception {
    IdentityHashMap<Integer, JComponent> myContentMap = extractMyContentField(multiPanel);
    myContentMap.put(selectKey, new JComponent() {} );
    // Putting something at this index already supresses the no preview label (because it never gets created), but we will also call cancel() just to be sure:

    setMyContentField(multiPanel, new IdentityHashMap<>(myContentMap) {
      @Override
      public JComponent get(Object key) {
        try {
          if ((Integer)key == selectKey) cancelPopup.run();
        } catch (Exception e) {} // We are in dispatch thread, catch everything
        return super.get(key);
      }
    });
  } // void tryToInstallSelectListener

  private static IdentityHashMap<Integer, JComponent> extractMyContentField(MultiPanel panel) {
    return MyContentFieldHolder.myContentFieldExtractor.apply(panel);
  }

  private static void setMyContentField(MultiPanel panel, IdentityHashMap<Integer, JComponent> newMap) {
    MyContentFieldHolder.myContentFieldExtractor.accept(panel, newMap);
  }

  @SuppressWarnings("rawtypes")
  private static class MyContentFieldHolder {
    private static final String myContent = "myContent";
    private static final Class<CardLayoutPanel> containingClass = CardLayoutPanel.class;
    private static final Class<IdentityHashMap> fieldType = IdentityHashMap.class;

    private static final FunctionBiConsumer<CardLayoutPanel, IdentityHashMap> myContentFieldExtractor = createMyContentGetterSetter();

    private static FunctionBiConsumer<CardLayoutPanel, IdentityHashMap> createMyContentGetterSetter() {
      try {
        return ReflectionUtils.getExactGetterSetter(containingClass, myContent, fieldType, new ReflectionUtils.ExactMultiInvocator<>() {
          @Override
          public IdentityHashMap invokeExactInvoke(MethodHandle handleToInvoke, CardLayoutPanel instance) throws Throwable {
            return (IdentityHashMap) handleToInvoke.invokeExact(instance);
          }

          @Override
          public void invokeExactInvoke(MethodHandle handleToInvoke, CardLayoutPanel instance, IdentityHashMap value) throws Throwable {
            handleToInvoke.invokeExact(instance, value);
          }
        });
      } catch (Exception e) {}
      return null;
    }
  } // class MyContentFieldHolder

} // class MultiPanelSelectListener
