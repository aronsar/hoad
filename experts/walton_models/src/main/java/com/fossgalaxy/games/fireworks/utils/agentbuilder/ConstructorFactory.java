package com.fossgalaxy.games.fireworks.utils.agentbuilder;

import com.fossgalaxy.games.fireworks.ai.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by webpigeon on 06/04/17.
 */
public class ConstructorFactory implements AgentFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstructorFactory.class);

    private final Class<? extends Agent> clazz;
    private final Constructor<?> constructor;
    private final Function<String, ?>[] converters;
    private final String name;


    public ConstructorFactory(Class<? extends Agent> clazz, Constructor<?> constructor, Function<String, ?>[] converters) {
        this(clazz, constructor, converters, clazz.getSimpleName());
    }

    public ConstructorFactory(Class<? extends Agent> clazz, Constructor<?> constructor, Function<String, ?>[] converters, String name) {
        this.clazz = clazz;
        this.constructor = constructor;
        this.converters = converters;
        this.name = name;
    }


    @Override
    public Agent build(String... args) {

        Object[] params = new Object[0];
        if (converters != null) {
            if (converters.length != args.length) {
                throw new IllegalArgumentException("incorrect argument count to build class:"+clazz+", given: "+Arrays.toString(args));
            }

            params = new Object[converters.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = converters[i].apply(args[i]);
            }
        }

        try {
            return (Agent)constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("error building from constructor {}", e);
        }
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    public String toString() {
        return String.format("agent factory for: %s - %s, %s", clazz.getSimpleName(), constructor, Arrays.toString(converters));
    }

}
