package com.revolut.interview;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.revolut.interview.model.Account;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * The main logic for actions with accounts.
 *
 * The Hazelcast has terrible API!
 * Every method returns {@link Object} and you should cast result value. =(
 * The Hazelcast has target java version 1.6 and in that reason reason there is no support lambda functions at all. =(
 *
 * @author Ivan Zemlyanskiy
 */
public class Bank {
    public static final String ACCOUNTS = "ACCOUNTS";
    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private final IMap<Long, Account> accounts;
    private final IAtomicLong nextId;

    public Bank(HazelcastInstance hazelcastInstance) {
        accounts = hazelcastInstance.getMap(ACCOUNTS);
        nextId = hazelcastInstance.getAtomicLong(ACCOUNT_ID);
    }

    public long createAccount(String name) {
        long id = nextId.getAndAdd(1);
        accounts.put(id, new Account(id, name));
        return id;
    }

    public boolean transfer(long donorId, long acceptorId, BigDecimal amount) {
        if (!accounts.containsKey(donorId) || !accounts.containsKey(acceptorId)) {
            return false;
        }

        long firstLock = Math.min(donorId, acceptorId);
        long secondLock = Math.max(donorId, acceptorId);

        try {
            accounts.lock(firstLock);
            accounts.lock(secondLock);

            Boolean subtractResult = (Boolean) accounts.executeOnKey(donorId, new AbstractEntryProcessor<Long, Account>() {
                @Override
                public Boolean process(Map.Entry<Long, Account> entry) {
                    Account donor = entry.getValue();
                    if (donor.getMoney().compareTo(amount) < 0) {
                        return false;
                    } else {
                        donor.setMoney(donor.getMoney().subtract(amount));
                        // if there is no this line account remains unchanged
                        entry.setValue(donor);
                        return true;
                    }
                }
            });

            if (subtractResult) {
                accounts.executeOnKey(acceptorId, new AbstractEntryProcessor<Long, Account>() {
                    @Override
                    public Object process(Map.Entry<Long, Account> entry) {
                        Account acceptor = entry.getValue();
                        acceptor.setMoney(acceptor.getMoney().add(amount));
                        // if there is no this line account remains unchanged
                        entry.setValue(acceptor);
                        return acceptor;
                    }
                });
            } else {
                return false;
            }


        } finally {
            accounts.unlock(secondLock);
            accounts.unlock(firstLock);
        }
        return true;

    }


    public BigDecimal refill(long id, BigDecimal amount) {
        try {
            accounts.lock(id);
            return (BigDecimal) accounts.executeOnKey(id, new AbstractEntryProcessor<Long, Account>() {
                @Override
                public BigDecimal process(Map.Entry<Long, Account> entry) {
                    Account account = entry.getValue();
                    if (amount.signum() < 0) {
                        return account.getMoney();
                    }
                    account.setMoney(account.getMoney().add(amount));
                    // if there is no this line account remains unchanged
                    entry.setValue(account);
                    return account.getMoney();
                }
            });
        } finally {
            accounts.unlock(id);
        }
    }

    public Optional<Account> findAccount(long id) {
        return Optional.ofNullable(accounts.get(id));
    }
}
