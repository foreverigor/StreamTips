package me.foreverigor.intellij.plugin.streamtips;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {

  public static <T> List<T> toList(Collection<T> collection) {
    return collection.stream().collect(Collectors.toList());
  }

  public static <T> T getFirst(Collection<T> collection) {
    return collection.iterator().next();
  }

  public interface UncertainHolder<T> {
    T get();

    <R> R doIfAvailable(Function<T, R> task);

    default void doIfAvailable(Consumer<T> voidTask) {
      doIfAvailable(t -> {
        voidTask.accept(t);
        return null;
      });
    }
  }

  private Utils() {}
} // class Utils
