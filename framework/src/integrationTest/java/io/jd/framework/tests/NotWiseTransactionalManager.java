package io.jd.framework.tests;

import jakarta.inject.Singleton;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

@Singleton
public class NotWiseTransactionalManager implements TransactionManager {

    @Override
    public void begin() throws NotSupportedException, SystemException {
        System.out.println("Begin");
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        System.out.println("Committing");
    }

    @Override
    public int getStatus() throws SystemException {
        return 0;
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return null;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {

    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        System.out.println("Rollbacking");
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {

    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {

    }

    @Override
    public Transaction suspend() throws SystemException {
        return null;
    }
}
