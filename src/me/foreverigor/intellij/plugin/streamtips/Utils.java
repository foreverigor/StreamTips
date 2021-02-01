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

  private Utils() {}
} // class Utils
