package me.foreverigor.utils;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface FunctionBiConsumer<T, R> extends Function<T, R>, BiConsumer<T, R> {
}
