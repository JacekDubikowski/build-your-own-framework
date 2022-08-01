package io.jd.framework;

import java.util.List;

import static java.util.function.Predicate.not;

class BaseBeanProvider implements BeanProvider {
    private final List<? extends BeanDefinition<?>> definitions;

    public BaseBeanProvider(List<? extends BeanDefinition<?>> definitions) {
        this.definitions = definitions;
    }

    @Override
    public <T> T provide(Class<T> beanType) {
        var beans = provideAll(beanType);
        if (beans.isEmpty()) {
            throw new IllegalStateException("No bean of given type: '%s'".formatted(beanType.getCanonicalName()));
        } else if (beans.size() > 1) {
            throw new IllegalStateException("More than one bean of given type: '%s'".formatted(beanType.getCanonicalName()));
        } else {
            return beans.get(0);
        }
    }

    @Override
    public <T> List<T> provideAll(Class<T> beanType) {
        var allBeans = definitions.stream().filter(def -> beanType.isAssignableFrom(def.type()))
                .map(def -> beanType.cast(def.create(this)))
                .toList();
        var interceptedTypes = allBeans.stream().filter(bean -> Intercepted.class.isAssignableFrom(bean.getClass()))
                .map(bean -> ((Intercepted) bean).interceptedType())
                .toList();
        return allBeans.stream().filter(not(bean -> interceptedTypes.contains(bean.getClass()))).toList();
    }

    @Override
    public <T> T provideExact(Class<T> beanType) {
        var beans = provideExactAll(beanType);
        if (beans.isEmpty()) {
            throw new IllegalStateException("No exact bean of given type: '%s'".formatted(beanType.getCanonicalName()));
        } else if (beans.size() > 1) {
            throw new IllegalStateException("More than one bean of exact type: '%s'".formatted(beanType.getCanonicalName()));
        } else {
            return beans.get(0);
        }
    }

    @Override
    public <T> List<T> provideExactAll(Class<T> beanType) {
        return definitions.stream().filter(def -> beanType.equals(def.type()))
                .map(def -> beanType.cast(def.create(this)))
                .toList();
    }
}
