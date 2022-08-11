package io.jd.framework.tests;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProviderTest {

    @Test
    void shouldCreateDefinitionForServices() {
        assertEquals(new $ServiceA$Definition().type(), ServiceA.class);
        assertEquals(new $ServiceB$Definition().type(), ServiceB.class);
        assertEquals(new $ServiceC$Definition().type(), ServiceC.class);
        assertEquals(new $RepositoryA$Intercepted$Definition().type(), RepositoryA$Intercepted.class);
        assertEquals(new $RepositoryA$Definition().type(), RepositoryA.class);
    }

    @Test
    void shouldProvideInstanceForServiceWithoutDependencies() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();

        ServiceA serviceA = beanProvider.provide(ServiceA.class);
        assertNotNull(serviceA);
        assertInstanceOf(ServiceA.class, serviceA);
    }

    @Test
    void shouldProvideInstanceForServiceWithOneDependency() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();

        ServiceB serviceB = beanProvider.provide(ServiceB.class);
        assertNotNull(serviceB);
        assertInstanceOf(ServiceB.class, serviceB);
    }

    @Test
    void shouldProvideInstanceForServiceWithTwoDependency() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();

        ServiceC serviceC = beanProvider.provide(ServiceC.class);
        assertNotNull(serviceC);
        assertInstanceOf(ServiceC.class, serviceC);
    }

    @Test
    void shouldProvideInterceptedInstance() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();

        RepositoryA repositoryA = beanProvider.provide(RepositoryA$Intercepted.class);
        assertNotNull(repositoryA);
        assertInstanceOf(RepositoryA$Intercepted.class, repositoryA);
    }

    @Test
    void shouldProvideTransactionallyInterceptedRepositoryWhenProvidingBaseOne() {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();

        RepositoryA repositoryA = beanProvider.provide(RepositoryA.class);
        assertNotNull(repositoryA);
        assertInstanceOf(RepositoryA$Intercepted.class, repositoryA);
    }
}
