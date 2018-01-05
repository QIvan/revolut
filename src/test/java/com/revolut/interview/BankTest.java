package com.revolut.interview;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * @author Ivan Zemlyanskiy
 */
public class BankTest {

    @Test
    public void accountCreateRefillAndTransfer() {
        HazelcastInstance hazelcastInstance = new TestHazelcastInstanceFactory().newHazelcastInstance();

        Bank bank = new Bank(hazelcastInstance);
        long donorId = bank.createAccount("Donor");
        long acceptorId = bank.createAccount("Acceptor");

        assertNotEquals(donorId, acceptorId);
        assertEquals(BigDecimal.ZERO, bank.findAccount(donorId).getMoney());
        assertEquals(BigDecimal.ZERO, bank.findAccount(acceptorId).getMoney());


        assertFalse(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(10)));

        BigDecimal refillAmount = BigDecimal.valueOf(20);
        bank.refill(donorId, refillAmount);
        assertEquals(refillAmount, bank.findAccount(donorId).getMoney());


        assertTrue(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(5)));
        assertEquals(BigDecimal.valueOf(5), bank.findAccount(acceptorId).getMoney());
        assertEquals(BigDecimal.valueOf(15), bank.findAccount(donorId).getMoney());

    }

}