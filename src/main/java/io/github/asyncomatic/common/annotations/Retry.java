package io.github.asyncomatic.common.annotations;

import io.github.asyncomatic.common.constants.Delay;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    int count();

    long delay() default 0;
    long units() default Delay.NONE;
}
