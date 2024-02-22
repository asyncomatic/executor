package io.github.asyncomatic.common.annotations;

import io.github.asyncomatic.common.constants.Condition;
import io.github.asyncomatic.common.constants.Delay;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ScheduleList.class)
public @interface Schedule {
    String method();

    long delay() default 0;
    long units() default Delay.NONE;

    int condition() default Condition.ANY;
}
