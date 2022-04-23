package io.jd.framework;

public interface BeanDefinition<T> {
    T create(BeanProvider beanProvider);

    Class<T> type();
}
