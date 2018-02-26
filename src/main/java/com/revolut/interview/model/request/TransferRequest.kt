package com.revolut.interview.model.request

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

/**
 * @author Ivan Zemlyanskiy
 */
class TransferRequest(@JsonAlias("acceptor")
                      val acceptorId: Long,
                      val amount: BigDecimal)
