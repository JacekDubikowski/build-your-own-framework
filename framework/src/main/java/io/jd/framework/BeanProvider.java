package io.jd.framework;

import java.util.Collection;

public interface BeanProvider {
    <T> T provide(Class<T> beanType);

    <T> Collection<T> provideAll(Class<T> beanType);
}
