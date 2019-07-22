package com.fossgalaxy.games.fireworks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a parameter that can be constructed.
 *
 * Created by webpigeon on 06/04/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface Parameter {
    int id();
    String func() default "";
    String description() default "";
}
