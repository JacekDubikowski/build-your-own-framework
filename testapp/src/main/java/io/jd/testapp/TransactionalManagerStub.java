package io.jd.testapp;

import jakarta.inject.Singleton;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

@Singleton
public class TransactionalManagerStub implements TransactionManager {

    @Override
    public void begin() {
        System.out.println("Begin transaction");
    }

    @Override
    public void commit() throws SecurityException, IllegalStateException {
        System.out.println("Commit transaction");
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException {
        System.out.println("Rollback transaction");
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
    public void setRollbackOnly() throws IllegalStateException {

    }

    @Override
    public void setTransactionTimeout(int seconds) {

    }

    @Override
    public Transaction suspend() {
        return null;
    }
}
