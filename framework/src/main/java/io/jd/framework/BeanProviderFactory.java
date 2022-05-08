package io.jd.framework;

import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.QueryFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.reflections.scanners.Scanners.SubTypes;

public class BeanProviderFactory {

    private static final QueryFunction<Store, Class<?>> TYPE_QUERY = SubTypes.of(BeanDefinition.class).asClass();

    public static BeanProvider getInstance(String... packages) {
        var reflections = new Reflections(new ConfigurationBuilder().forPackages("io.jd").forPackages(packages));

        var definitions = reflections
                .get(TYPE_QUERY)
                .stream()
                .filter(def -> def.getDeclaredConstructors().length == 1)
                .map(BeanProviderFactory::getInstance)
                .flatMap(Optional::stream)
                .toList();
        return new BaseBeanProvider(definitions);
    }

    private static Optional<? extends BeanDefinition<?>> getInstance(Class<?> e) {
        try {
            BeanDefinition<?> value = (BeanDefinition<?>) e.getDeclaredConstructors()[0].newInstance();
            return Optional.of(value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            //TODO: Handle error properly
            ex.printStackTrace();
            return Optional.empty();
        }
    }
}
