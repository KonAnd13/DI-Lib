package tech.itpark.di;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Container {
    private final Map<Class<?>, Object> objects = new HashMap<>();
    private final Set<Class<?>> definitions = new HashSet<>();

    public void register(Class<?>... definitions) {
        String badDefinitions = Arrays.stream(definitions)
                .filter(o -> o.getDeclaredConstructors().length != 1)
                .map(o -> o.getName())
                .collect(Collectors.joining(", "));
        if (!badDefinitions.isEmpty()) {
            throw new AmbiguousConstructorException(badDefinitions);
        }

        this.definitions.addAll(Arrays.asList(definitions));
    }

    public void wire() {
        HashSet<Class<?>> todo = new HashSet<>(definitions);
        if (todo.size() == 0) {
            return;
        }

        while (objects.size() != definitions.size()) {
            for (Class<?> clazz : todo) {
                final var constructor = clazz.getDeclaredConstructors()[0];
                if (constructor.getParameterCount() == 0) {
                    try {
                        constructor.setAccessible(true);
                        Object object = constructor.newInstance();
                        objects.put(object.getClass(), object);
                    } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new ObjectInstantiationException(e);
                    }
                } else {
                    if (objects.keySet().containsAll(Arrays.asList(constructor.getParameterTypes()))) {
                        try {
                            constructor.setAccessible(true);
                            Object object = constructor.newInstance(Arrays.stream(constructor.getParameters())
                                    .map(p -> objects.get(p.getType()))
                                    .toArray()
                            );
                            objects.put(object.getClass(), object);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            throw new ObjectInstantiationException(e);
                        }
                    }
                }
            }
            todo.removeAll(objects.keySet());
        }

        if (todo.size() == 0) {
            return;
        }
        String unmet = todo.stream()
                .map(o -> o.getName())
                .collect(Collectors.joining(", "));
        throw new UnmetDependenciesException(unmet);
    }
}
