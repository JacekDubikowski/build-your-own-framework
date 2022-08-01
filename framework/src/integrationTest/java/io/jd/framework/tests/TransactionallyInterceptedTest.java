package io.jd.framework.tests;

import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionallyInterceptedTest {

    NotWiseTransactionalManager manager = new NotWiseTransactionalManager();
    TestRepository repository = new TestRepository$Intercepted(manager, new ServiceA());

    @AfterEach
    void tearDown() {
        manager.reset();
        repository.shouldThrow = false;
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
        repository.shouldThrow = true;

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
    boolean shouldThrow = false;

    private final ServiceA serviceA;

    TestRepository(ServiceA serviceA) {
        this.serviceA = serviceA;
    }

    @Transactional
    void transactionalMethod() {
        if (shouldThrow) {
            throw new RuntimeException();
        }
    }

    void nonTransactionalMethod() {

    }
}
