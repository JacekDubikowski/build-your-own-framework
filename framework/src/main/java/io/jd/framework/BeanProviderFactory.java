package io.jd.framework;

import static org.reflections.scanners.Scanners.SubTypes;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.QueryFunction;

public class BeanProviderFactory {

    private static final QueryFunction<Store, Class<?>> TYPE_QUERY = SubTypes.of(BeanDefinition.class).asClass();

    public static BeanProvider getInstance(String... packages) {
        ConfigurationBuilder reflectionsConfig = new ConfigurationBuilder()
                .forPackage("io.jd")
                .forPackages(packages)
                .filterInputsBy(createPackageFilter(packages));
        var reflections = new Reflections(reflectionsConfig);
        var definitions = definitions(reflections);
        return new BaseBeanProvider(definitions);
    }

    private static FilterBuilder createPackageFilter(String[] packages) {
        var filter = new FilterBuilder().includePackage("io.jd");
        Arrays.asList(packages).forEach(filter::includePackage);
        return filter;
    }

    private static List<? extends BeanDefinition<?>> definitions(Reflections reflections) {
        return reflections
                .get(TYPE_QUERY)
                .stream()
                .map(BeanProviderFactory::getInstance)
                .toList();
    }

    private static BeanDefinition<?> getInstance(Class<?> e) {
        try {
            return (BeanDefinition<?>) e.getDeclaredConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new FailedToInstantiateBeanDefinitionException(e, ex);
        }
    }
}
