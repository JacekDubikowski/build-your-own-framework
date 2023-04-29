package io.jd.framework.tests;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;
import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebHandlerTest {

    @Test
    void shouldProvideHandlerInt2WithExpectedBehaviour() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();
        var handler = beanProvider.provide(ExampleController$getInt$2$handler.class);

        assertEquals(HttpMethod.GET, handler.method());
        assertEquals("/int2", handler.path());
        assertEquals(MediaType.TEXT_PLAIN, handler.produce());
    }

    @Test
    void shouldProvideHandlerInt1WithExpectedBehaviour() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();
        var handler = beanProvider.provide(ExampleController$getInt$1$handler.class);

        assertEquals(HttpMethod.GET, handler.method());
        assertEquals("/int", handler.path());
        assertEquals("application/json+framework", handler.produce());
    }

    @Test
    void shouldProvideHandlerInt3WithExpectedBehaviour() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();
        var handler = beanProvider.provide(ExampleController$getIntFromString$1$handler.class);

        assertEquals(HttpMethod.POST, handler.method());
        assertEquals("/int3", handler.path());
        assertEquals(MediaType.APPLICATION_JSON, handler.produce());
    }

    @Test
    void shouldProvideHandlerVoidWithExpectedBehaviour() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();
        var handler = beanProvider.provide(ExampleController$doSomething$1$handler.class);

        assertEquals(HttpMethod.POST, handler.method());
        assertEquals("/void", handler.path());
        assertEquals(MediaType.APPLICATION_JSON, handler.produce());
    }

}
