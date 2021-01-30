package me.foreverigor.intellij.platform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can only be run on the dispatch thread
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface OnDispatchThread {
}
