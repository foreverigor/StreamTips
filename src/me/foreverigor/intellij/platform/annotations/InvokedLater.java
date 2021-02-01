package me.foreverigor.intellij.platform.annotations;

import java.lang.annotation.*;

/**
 * This code will be called later from somewhere. Usually this means that it should be checked whether the task
 * is still applicable/ relevant
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface InvokedLater {
}
