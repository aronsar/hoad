package com.fossgalaxy.games.fireworks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag a constructor as being used for agent creation.
 *
 * Created by webpigeon on 06/04/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface AgentConstructor {

    String value();
}
