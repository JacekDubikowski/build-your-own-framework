package io.jd.framework;

import java.util.function.Function;

public sealed interface ScopeProvider<T> extends Function<BeanProvider, T> {

    static <T> ScopeProvider<T> singletonScope(Function<BeanProvider, T> delegate) {
        return new SingletonProvider<>(delegate);
    }
}

final class SingletonProvider<T> implements ScopeProvider<T> {
    private final Function<BeanProvider, T> delegate;
    private volatile T value;

    SingletonProvider(Function<BeanProvider, T> delegate) {
        this.delegate = delegate;
    }

    public synchronized T apply(BeanProvider beanProvider) {
        if (value == null) {
            value = delegate.apply(beanProvider);
        }
        return value;
    }
}
