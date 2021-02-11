package me.foreverigor.intellij.platform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Required for the plugin to function.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.FIELD})
public @interface Required {
}
