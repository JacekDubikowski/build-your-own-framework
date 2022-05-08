package io.jd.framework;

public interface ApplicationContext {
    <T> T provide(Class<T> beanType);
}
