package com.revolut.interview;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * @author Ivan Zemlyanskiy
 */
public class BankTest {


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testAccountCreateRefillAndTransfer() {
        Bank bank = new Bank();
        long donorId = bank.createAccount("Donor").getId();
        long acceptorId = bank.createAccount("Acceptor").getId();

        assertNotEquals(donorId, acceptorId);
        assertEquals(BigDecimal.ZERO, bank.findAccount(donorId).getMoney());
        assertEquals(BigDecimal.ZERO, bank.findAccount(acceptorId).getMoney());


        assertFalse(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(10)));
        assertFalse(bank.transfer(donorId, donorId + acceptorId + 1, BigDecimal.valueOf(10)));
        assertFalse(bank.transfer(donorId + acceptorId + 1, acceptorId, BigDecimal.valueOf(10)));

        BigDecimal refillAmount = BigDecimal.valueOf(20);
        bank.refill(donorId, refillAmount);
        assertEquals(refillAmount, bank.findAccount(donorId).getMoney());


        assertTrue(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(5)));
        assertEquals(BigDecimal.valueOf(5), bank.findAccount(acceptorId).getMoney());
        assertEquals(BigDecimal.valueOf(15), bank.findAccount(donorId).getMoney());

    }

    @Test
    public void refillOnNegativeAmountShouldNotChangeAccount() throws Exception {

        Bank bank = new Bank();
        long id = bank.createAccount("Account").getId();

        bank.refill(id, new BigDecimal(-10));

        assertEquals(BigDecimal.ZERO, bank.findAccount(id).getMoney());
    }

    @Test
    public void transferOnNegativeAmountShouldFailAndDoNothing() throws Exception {

        Bank bank = new Bank();
        long donorId = bank.createAccount("Donor").getId();
        long acceptorId = bank.createAccount("Acceptor").getId();

        BigDecimal donorAmount = bank.refill(donorId, BigDecimal.TEN).getMoney();

        assertFalse(bank.transfer(donorId, acceptorId, BigDecimal.valueOf(-10)));

        assertEquals(donorAmount, bank.findAccount(donorId).getMoney());
        assertEquals(BigDecimal.ZERO, bank.findAccount(acceptorId).getMoney());
    }

    @Test
    public void transferTheSameAccount() {

        Bank bank = new Bank();
        long id = bank.createAccount("The same").getId();

        BigDecimal amount = bank.refill(id, BigDecimal.TEN).getMoney();

        assertTrue(bank.transfer(id, id, amount));

        assertEquals(amount, bank.findAccount(id).getMoney());
    }
}