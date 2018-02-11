package com.revolut.interview.model.request

import com.fasterxml.jackson.annotation.JsonAlias

/**
 * @author Ivan Zemlyanskiy
 */
class TransferRequest(@JsonAlias("donor")
                      val donorId: Long,
                      @JsonAlias("acceptor")
                      val acceptorId: Long,
                      val amount: String)
