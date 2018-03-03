package com.revolut.interview

import io.ktor.application.*
import io.ktor.features.DefaultHeaders
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * @author Ivan Zemlyanskiy
 */
fun main(args: Array<String>) {

    val server = embeddedServer(Netty, 9000) {
        bankApplication(Bank())
    }

    server.start(wait = true)
}