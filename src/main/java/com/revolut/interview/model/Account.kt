package com.revolut.interview.model

import java.io.Serializable
import java.math.BigDecimal

/**
 * @author Ivan Zemlyanskiy
 */
data class Account(
        val id: Long,
        val name: String,
        var money: BigDecimal = BigDecimal.ZERO
) : Serializable
