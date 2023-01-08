package io.jd.framework.webapp;

import java.util.function.Function;

record IndexedValue<T>(int index, T value) {
    static <T> Function<T, IndexedValue<T>> indexed() {
        return new Function<>() {
            int index = 1;

            @Override
            public IndexedValue<T> apply(T t) {
                return new IndexedValue<T>(index++, t);
            }
        };
    }
}
