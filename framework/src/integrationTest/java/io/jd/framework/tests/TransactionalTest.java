package io.jd.framework.tests;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionalTest {

    BeanProvider beanProvider = BeanProviderFactory.getInstance();
    TestRepository repository = beanProvider.provide(TestRepository.class);
    NotWiseTransactionalManager manager = beanProvider.provide(NotWiseTransactionalManager.class);

    @AfterEach
    void tearDown() {
        manager.reset();
        TestRepository.shouldThrow = false;
    }

    @Test
    void shouldCreateTransactionalVersionThatWouldBeginAndCommitTransaction() {
        repository.transactionalMethod();

        assertEquals(1, manager.beginCounter().get());
        assertEquals(1, manager.commitCounter().get());
        assertEquals(0, manager.rollbackCounter().get());
    }

    @Test
    void shouldCreateTransactionalVersionThatWouldBeginAndRollbackTransactionIfThereWasError() {
        TestRepository.shouldThrow = true;

        assertThrows(RuntimeException.class, () -> repository.transactionalMethod());

        assertEquals(1, manager.beginCounter().get());
        assertEquals(0, manager.commitCounter().get());
        assertEquals(1, manager.rollbackCounter().get());
    }

    @Test
    void shouldNotUseTransactionMechanismWhenCalledMethodIsNotAnnotatedAsTransactional() {
        repository.nonTransactionalMethod();

        assertEquals(0, manager.beginCounter().get());
        assertEquals(0, manager.commitCounter().get());
        assertEquals(0, manager.rollbackCounter().get());
    }
}

@Singleton
class TestRepository {

    static volatile boolean shouldThrow = false;

    @Transactional
    void transactionalMethod() {
        if (shouldThrow) {
            throw new RuntimeException();
        }
    }

    void nonTransactionalMethod() {

    }
}
