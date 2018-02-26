package com.revolut.interview

import com.revolut.interview.model.Account
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * The main logic for actions with accounts.

 * @author Ivan Zemlyanskiy
 */
class Bank {
    companion object {
        private val log = LoggerFactory.getLogger(Bank::class.java)
    }

    private val nextId: AtomicLong = AtomicLong()
    private val accounts: MutableMap<Long, Account> = ConcurrentHashMap()

    fun createAccount(name: String): Account {
        val id = nextId.getAndIncrement()
        val result = Account(id, name)
        accounts[id] = result

        return result
    }

    fun transfer(donorId: Long, acceptorId: Long, amount: BigDecimal): Boolean {
        if (amount <= BigDecimal.ZERO) {
            log.debug("Transfer amount is negative")
            return false
        }

        val donor = accounts[donorId] ?: return false
        val acceptor = accounts[acceptorId] ?: return false

        if (donor.money < amount) {
            log.debug("Insufficient funds")
            return false
        }


        val (firstLock, secondLock) =
                if (donor.id < acceptor.id)
                    Pair(donor, acceptor)
                else
                    Pair(acceptor, donor)

        synchronized(firstLock) {
            synchronized(secondLock) {
                donor.money = donor.money - amount
                acceptor.money = acceptor.money + amount
            }
        }
        return true

    }


    fun refill(id: Long, amount: BigDecimal): Account? {
        if (amount <= BigDecimal.ZERO) {
            return null
        }
        accounts[id]?.let {
            synchronized(it) {
                it.money = it.money + amount
                return it
            }
        }
        return null
    }

    fun findAccount(id: Long): Account? {
        accounts[id]?.let {
            synchronized(it) {
                return it
            }
        }
        return null
    }


}
