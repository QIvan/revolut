package com.revolut.interview;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * @author Ivan Zemlyanskiy
 */
public class BankTest {

    private HazelcastInstance hazelcastInstance;

    @Before
    public void setUp() throws Exception {
        hazelcastInstance = new TestHazelcastInstanceFactory().newHazelcastInstance();
    }

    @Test
    public void testAccountCreateRefillAndTransfer() {
        Bank bank = new Bank(hazelcastInstance);
        long donorId = bank.createAccount("Donor");
        long acceptorId = bank.createAccount("Acceptor");

        assertNotEquals(donorId, acceptorId);
        assertEquals(BigDecimal.ZERO, bank.findAccount(donorId).get().getMoney());
        assertEquals(BigDecimal.ZERO, bank.findAccount(acceptorId).get().getMoney());


        assertFalse(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(10)));
        assertFalse(bank.transfer(donorId, donorId + acceptorId + 1, BigDecimal.valueOf(10)));
        assertFalse(bank.transfer(donorId + acceptorId + 1, acceptorId, BigDecimal.valueOf(10)));

        BigDecimal refillAmount = BigDecimal.valueOf(20);
        bank.refill(donorId, refillAmount);
        assertEquals(refillAmount, bank.findAccount(donorId).get().getMoney());


        assertTrue(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(5)));
        assertEquals(BigDecimal.valueOf(5), bank.findAccount(acceptorId).get().getMoney());
        assertEquals(BigDecimal.valueOf(15), bank.findAccount(donorId).get().getMoney());

    }

    @Test
    public void refillOnNegativeAmountShouldNotChangeAccount() throws Exception {

        Bank bank = new Bank(hazelcastInstance);
        long id = bank.createAccount("Account");

        bank.refill(id, new BigDecimal(-10));

        assertEquals(BigDecimal.ZERO, bank.findAccount(id).get().getMoney());
    }

    @Test
    public void transferOnNegativeAmountShouldFailAndDoNothing() throws Exception {

        Bank bank = new Bank(hazelcastInstance);
        long donorId = bank.createAccount("Donor");
        long acceptorId = bank.createAccount("Acceptor");

        BigDecimal donorAmount = bank.refill(donorId, BigDecimal.TEN);

        assertFalse(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(-10)));

        assertEquals(donorAmount, bank.findAccount(donorId).get().getMoney());
        assertEquals(BigDecimal.ZERO, bank.findAccount(acceptorId).get().getMoney());
    }

    @Test
    public void transferTheSameAccount() {

        Bank bank = new Bank(hazelcastInstance);
        long id = bank.createAccount("The same");

        BigDecimal amount = bank.refill(id, BigDecimal.TEN);

        assertTrue(bank.transfer(id, id, amount));

        assertEquals(amount, bank.findAccount(id).get().getMoney());
    }
}