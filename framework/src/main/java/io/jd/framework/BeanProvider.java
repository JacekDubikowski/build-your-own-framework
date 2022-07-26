package io.jd.framework;

public interface BeanProvider {
    <T> T provide(Class<T> beanType);

    <T> T provideExact(Class<T> beanType);

    <T> Iterable<T> provideAll(Class<T> beanType);

    <T> Iterable<T> provideExactAll(Class<T> beanType);
}
