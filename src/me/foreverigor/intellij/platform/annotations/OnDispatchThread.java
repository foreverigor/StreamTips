package me.foreverigor.intellij.platform.annotations;

import java.lang.annotation.*;

/**
 * Should be run from the dispatch thread
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface OnDispatchThread {
}
