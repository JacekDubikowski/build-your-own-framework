package io.jd.framework.tests;

import jakarta.inject.Singleton;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class NotWiseTransactionalManager implements TransactionManager {

    private final AtomicInteger beginCounter = new AtomicInteger();
    private final AtomicInteger commitCounter = new AtomicInteger();
    private final AtomicInteger rollbackCounter = new AtomicInteger();

    @Override
    public void begin() {
        beginCounter.incrementAndGet();
    }

    @Override
    public void commit() throws SecurityException, IllegalStateException {
        commitCounter.incrementAndGet();
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public Transaction getTransaction() {
        return null;
    }

    @Override
    public void resume(Transaction tobj) throws IllegalStateException {

    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException {
        rollbackCounter.incrementAndGet();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {

    }

    @Override
    public void setTransactionTimeout(int seconds) {

    }

    @Override
    public Transaction suspend() {
        return null;
    }

    public AtomicInteger beginCounter() {
        return beginCounter;
    }

    public AtomicInteger commitCounter() {
        return commitCounter;
    }

    public AtomicInteger rollbackCounter() {
        return rollbackCounter;
    }

    void reset() {
        commitCounter.set(0);
        rollbackCounter.set(0);
        beginCounter.set(0);
    }
}
