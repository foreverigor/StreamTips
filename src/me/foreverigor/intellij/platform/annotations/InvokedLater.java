package me.foreverigor.intellij.platform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This code will be called later from somewhere. Usually this means that it should be checked whether the task
 * is still applicable/ relevant
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER})
public @interface InvokedLater {
}
