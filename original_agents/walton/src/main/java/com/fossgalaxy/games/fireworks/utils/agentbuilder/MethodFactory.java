package com.fossgalaxy.games.fireworks.utils.agentbuilder;

import com.fossgalaxy.games.fireworks.ai.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by piers on 07/04/17.
 */
public class MethodFactory implements AgentFactory{
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodFactory.class);
    private final Class<?> clazz;
    private final Method method;
    private final Function<String, ?>[] converters;
    private final String name;

    public MethodFactory(Class<?> clazz, Method method, Function<String, ?>[] converters) {
        this(clazz, method, converters, clazz.getSimpleName());
    }

    public MethodFactory(Class<?> clazz, Method method, Function<String, ?>[] converters, String name) {
        this.clazz = clazz;
        this.method = method;
        this.converters = converters;
        this.name = name;
    }

    @Override
    public Agent build(String[] args) {
        Object[] params = new Object[0];
        if(converters != null){
            if (converters.length != args.length) {
                throw new IllegalArgumentException("incorrect argument count to build class:"+clazz+", given: "+Arrays.toString(args));
            }

            params = new Object[converters.length];
            for(int i = 0; i < params.length; i++){
                params[i] = converters[i].apply(args[i]);
            }
        }


        try {
            return (Agent) method.invoke(null, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("error building from method: {}", e);
        }
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    public String toString() {
        return String.format("agent factory for: %s - %s, %s", clazz.getSimpleName(), method, Arrays.toString(converters));
    }
}
