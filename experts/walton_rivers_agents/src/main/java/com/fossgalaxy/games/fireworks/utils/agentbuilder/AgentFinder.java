package com.fossgalaxy.games.fireworks.utils.agentbuilder;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.mcs.MonteCarloSearch;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.mcts.NoisyPredictor;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;
import com.fossgalaxy.games.fireworks.ai.vanDenBergh.VanDenBergh;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.annotations.Parameter;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by webpigeon on 06/04/17.
 */
public class AgentFinder {
    private static final Logger logger = LoggerFactory.getLogger(AgentFinder.class);
    private final Map<Class<?>, Function<String, ?>> converters;
    private final Map<String, AgentFactory> knownFactories;

    // have we already scanned for agents?
    private boolean hasScanned;

    public AgentFinder() {
        this.converters = new HashMap<>();
        this.knownFactories = new HashMap<>();
        this.hasScanned = false;

        buildConverters();
    }

    public static void main(String[] args) {
        AgentFinder finder = new AgentFinder();

        // as a test build our modified MCTS agent
        MCTS rmhc = (MCTS) finder.buildAgent("mcts", "50", "6", "10");
        System.out.println(rmhc);
        VanDenBergh van = (VanDenBergh) finder.buildAgent("vandenbergh", "0.6", "1.0", "NEXT_USEFUL_THEN_MOST_CARDS",
                "MOST_CERTAIN_IS_USELESS");
        System.out.println(van);

        ProductionRuleAgent vandenbergh = (ProductionRuleAgent) finder.buildAgent("VanDenBergh", "0.6", "1.0",
                "NEXT_USEFUL_THEN_MOST_CARDS", "MOST_CERTAIN_IS_USELESS");
        System.out.println(vandenbergh);

        NoisyPredictor predictor = (NoisyPredictor) finder.buildAgent("noisy", "0.9", "iggi");
        System.out.println(predictor);

        /*
         * MonteCarloSearch mcs = (MonteCarloSearch) finder.buildAgent("mcs", "iggi");
         * System.out.println(mcs);
         */
        finder.knownFactories.values().forEach(System.out::println);
    }

    /**
     * A default list of converters that we understand.
     */
    private void buildConverters() {
        converters.put(String.class, Function.identity());
        converters.put(Integer.class, Integer::parseInt);
        converters.put(int.class, Integer::parseInt);
        converters.put(Double.class, Double::parseDouble);
        converters.put(double.class, Double::parseDouble);
        converters.put(Float.class, Float::parseFloat);
        converters.put(float.class, Float::parseFloat);
        converters.put(Boolean.class, Boolean::parseBoolean);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(int[].class, AgentFinder::parseIntArray);
        converters.put(Agent.class, AgentUtils::buildAgent);
    }

    public static int[] parseIntArray(String data) {
        String[] args = data.split(",");
        int[] argInt = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            argInt[i] = Integer.parseInt(args[i]);
        }
        return argInt;
    }

    /**
     * Allow creation of custom converters.
     *
     * @param clazz     the class to convert
     * @param converter the converter to use
     * @param <T>       the type that we expect to convert to.
     */
    public <T> void addConverter(Class<T> clazz, Function<String, T> converter) {
        converters.put(clazz, converter);
    }

    /**
     * Allow manual insertion of factories.
     *
     * @param name    the name of the factory
     * @param factory the method that creates the agent
     */
    public void addFactory(String name, AgentFactory factory) {
        knownFactories.put(name, factory);
    }

    /**
     * Generate an agent with a given name from this factory
     *
     * @param name the name of the agent
     * @param args the arguments to pass to the factory
     * @return The constructed agent
     */
    public Agent buildAgent(String name, String... args) {
        if (!hasScanned) {
            scanForAgents();
        }

        AgentFactory factory = knownFactories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown factory type: " + name);
        }

        return factory.build(args);
    }

    /**
     * A lazy-loaded class scanner.
     * <p>
     * This scans the whole classpath for classes that extend agent, and builds
     * factories for them.
     */
    private void scanForAgents() {

        // ensure that we do not scan more than once - once is enough.
        if (hasScanned) {
            return;
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(new MethodAnnotationsScanner(), new SubTypesScanner(), new TypeAnnotationsScanner())
                .setExpandSuperTypes(false));

        // find all subtypes of the agent class
        scanForConstructors(reflections);

        // find all annotated static methods
        scanForStaticMethods(reflections);

        // ensure we do not scan again
        hasScanned = true;
    }

    private void scanForStaticMethods(Reflections reflections) {
        Set<Method> methods = reflections.getMethodsAnnotatedWith(AgentBuilderStatic.class);

        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (!(Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))) {
                continue;
            }

            try {
                AgentFactory factory = buildFactory(method);
                knownFactories.put(factory.name(), factory);
                // FIXME
                System.out.println("Name: " + factory.name());
            } catch (IllegalArgumentException ex) {
                logger.error("Failed to parse static method: " + method.getDeclaringClass() + "->" + method.getName());
            }

        }
    }

    private void scanForConstructors(Reflections reflections) {
        Set<Class<? extends Agent>> agentClazzes = reflections.getSubTypesOf(Agent.class);
        for (Class<? extends Agent> agentClazz : agentClazzes) {

            // skip the class if it is abstract or not public.
            int classMods = agentClazz.getModifiers();
            if (Modifier.isAbstract(classMods) || !Modifier.isPublic(classMods)) {
                continue;
            }

            try {
                AgentFactory factory = buildFactory(agentClazz);
                knownFactories.put(factory.name(), factory);
            } catch (IllegalArgumentException ex) {
                logger.error("Failed to create agent " + agentClazz, ex);
            }
        }
    }

    public AgentFactory buildFactory(Method method) {
        AgentBuilderStatic agentBuilder = method.getDeclaredAnnotation(AgentBuilderStatic.class);
        String name = agentBuilder.value();
        HashMap<Integer, Parameter> parameters = new HashMap<>();
        for (Parameter p : method.getAnnotationsByType(Parameter.class)) {
            if (!parameters.containsKey(p.id())) {
                parameters.put(p.id(), p);
            }
        }
        Function<String, ?>[] convertersInst = getConverters(method.getDeclaringClass(), method.getParameterTypes(),
                parameters);
        return new MethodFactory(method.getDeclaringClass(), method, convertersInst, name);
    }

    public AgentFactory buildFactory(Class<? extends Agent> agentClazz) {

        Constructor<?> bestMatch = null;

        Constructor<?>[] constructors = agentClazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
                bestMatch = constructor;
            } else {
                AgentConstructor builder = constructor.getAnnotation(AgentConstructor.class);
                if (builder == null) {
                    continue;
                }

                String name = "".equals(builder.value()) ? agentClazz.getSimpleName() : builder.value();
                bestMatch = constructor;

                HashMap<Integer, Parameter> parameters = new HashMap<>();
                for (Parameter p : constructor.getAnnotationsByType(Parameter.class)) {
                    if (!parameters.containsKey(p.id())) {
                        parameters.put(p.id(), p);
                    }
                }

                Function<String, ?>[] convertersInst = getConverters(agentClazz, constructor.getParameterTypes(),
                        parameters);

                return new ConstructorFactory(agentClazz, constructor, convertersInst, name);
            }
        }

        if (bestMatch == null) {
            throw new IllegalArgumentException(
                    "You must either annotate a constructor or provide a public no-args constructor");
        }

        return new ConstructorFactory(agentClazz, bestMatch, null);
    }

    private Function<String, ?>[] getConverters(Class<?> agentClazz, Class<?>[] params,
            HashMap<Integer, Parameter> parameters) {
        Function<String, ?>[] convertersInst = (Function[]) Array.newInstance(Function.class, params.length);
        for (int i = 0; i < params.length; i++) {
            if (parameters.containsKey(i)) {
                Parameter parameter = parameters.get(i);
                try {
                    Method methodWithThatName = agentClazz.getMethod(parameter.func(), String.class);
                    if (Modifier.isPublic(methodWithThatName.getModifiers())
                            && Modifier.isStatic(methodWithThatName.getModifiers())) {

                        if (!methodWithThatName.getReturnType().isAssignableFrom(params[i])) {
                            throw new IllegalArgumentException("you said params " + i + " was a " + params[i]
                                    + " but the converter wants to give me a " + methodWithThatName.getReturnType());
                        }
                        convertersInst[i] = (s) -> getConverter(methodWithThatName, s);
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            } else {
                // Try to handle enums with a default
                if (params[i].isEnum()) {
                    final Class enumClass = params[i];
                    convertersInst[i] = (s) -> Enum.valueOf(enumClass, s);

                } else {
                    convertersInst[i] = converters.get(params[i]);
                }
            }
        }

        return convertersInst;
    }

    private Object getConverter(Method methodWithThatName, String s) {
        try {
            return methodWithThatName.invoke(null, s);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("error invoking method {}", e);
        }
        return null;
    }

    public Map<String, AgentFactory> getFactories() {
        return Collections.unmodifiableMap(knownFactories);
    }
}
