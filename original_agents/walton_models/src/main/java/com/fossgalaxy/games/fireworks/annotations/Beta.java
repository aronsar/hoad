package com.fossgalaxy.games.fireworks.annotations;

import java.lang.annotation.Inherited;

/**
 * Created by piers on 05/04/17.
 *
 * Signals that something is not to be used as it is currently under construction
 */
@Inherited
public @interface Beta {

    String responsible() default "";
    String reason() default "";
}
