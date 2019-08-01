package com.fossgalaxy.games.fireworks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by piers on 07/04/17.
 *
 * Must only be used on a static method that returns Agent
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AgentBuilderStatic {
    String value();
}
