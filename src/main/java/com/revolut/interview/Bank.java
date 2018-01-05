package com.revolut.interview;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import com.hazelcast.map.AbstractEntryProcessor;
import com.revolut.interview.model.Account;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author Ivan Zemlyanskiy
 */
public class Bank {
    public static final String ACCOUNTS = "ACCOUNTS";
    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private final HazelcastInstance hazelcast;
    private final IMap<Long, Account> accounts;
    private final IAtomicLong nextId;

    public Bank(HazelcastInstance hazelcastInstance) {
        hazelcast = hazelcastInstance;
        accounts = hazelcast.getMap(ACCOUNTS);
        nextId = hazelcast.getAtomicLong(ACCOUNT_ID);
    }

    public long createAccount(String name) {
        long id = nextId.getAndAdd(1);
        accounts.put(id, new Account(id, name));
        return id;
    }

    public boolean transfer(long donorId, long acceptorId, BigDecimal amount) {
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


    public void refill(long id, BigDecimal amount) {
        accounts.executeOnKey(id, new AbstractEntryProcessor<Long, Account>() {
            @Override
            public Object process(Map.Entry<Long, Account> entry) {
                Account account = entry.getValue();
                account.setMoney(account.getMoney().add(amount));
                // if there is no this line account remains unchanged
                entry.setValue(account);
                return entry;
            }
        });
    }

    public Account findAccount(long id) {
        return accounts.get(id);
    }
}
