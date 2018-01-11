package com.revolut.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revolut.interview.config.HttpConfig;
import com.revolut.interview.model.Account;
import com.revolut.interview.model.request.CreateRequest;
import com.revolut.interview.model.request.RefillRequest;
import com.revolut.interview.model.request.TransferRequest;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.xnio.Options;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Deque;
import java.util.Optional;

/**
 * @author Ivan Zemlyanskiy
 */
@Slf4j
public class HttpBankServer {

    private final Undertow httpServer;
    private final Bank bank;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public HttpBankServer(Bank bank, int portIncrement) {
        this.bank = bank;
//        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        HttpConfig httpConfig = ConfigFactory.create(HttpConfig.class);

        int httpPort = httpConfig.port() + portIncrement;
        httpServer = Undertow.builder()
                //we have hazelcast's thread pool already
                .setWorkerOption(Options.WORKER_TASK_LIMIT, 10)
                .addHttpListener(httpPort, httpConfig.host())
                .setHandler(Handlers.exceptionHandler(
                        Handlers.path()
                                .addPrefixPath("/account", Handlers.routing()
                                        .get("/info/{id}", exchange -> exchange.dispatch(this::info))
                                        .post("/create", exchange -> exchange.dispatch(this::create))
                                        .post("/refill", exchange -> exchange.dispatch(this::refill))
                                        .post("/transfer", exchange -> exchange.dispatch(this::transfer))
                                        .setFallbackHandler(this::notFoundHandler)
                                )
                        ).addExceptionHandler(Throwable.class, exchange -> {
                            Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);
                            log.error("", throwable);
                            exchange.setStatusCode(500);
                        })
                )
                .build();
    }

    private long extractId(HttpServerExchange exchange) {
        Deque<String> idParams = exchange.getQueryParameters().get("id");
        return Long.parseLong(idParams.poll());
    }

    private Optional<Account> findAccount(HttpServerExchange exchange, long id) {
        Optional<Account> findResult = bank.findAccount(id);
        if (!findResult.isPresent()) {
            exchange.setStatusCode(StatusCodes.NO_CONTENT);
            exchange.getResponseSender().send("{}");
            return Optional.empty();
        }
        return findResult;
    }

    private void info(HttpServerExchange exchange) {
        long id = extractId(exchange);

        findAccount(exchange, id).ifPresent(account -> {
            try {
                exchange.startBlocking();
                jsonMapper.writeValue(exchange.getOutputStream(), account);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <A> A readBody(HttpServerExchange exchange, Class<A> type) throws IOException {
        final A result;
        exchange.startBlocking();
        try (InputStream inputStream = exchange.getInputStream()) {
            result = jsonMapper.readValue(inputStream, type);
        }
        return result;
    }

    private void refill(HttpServerExchange exchange) throws IOException {
        RefillRequest request = readBody(exchange, RefillRequest.class);
        bank.refill(request.getId(), new BigDecimal(request.getAmount()));
    }

    private void transfer(HttpServerExchange exchange) throws IOException {
        TransferRequest request = readBody(exchange, TransferRequest.class);
        boolean transferSuccess = bank.transfer(request.getDonorId(),
                                                request.getAcceptorId(),
                                                new BigDecimal(request.getAmount()));
        if (!transferSuccess) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }
    }

    private void create(HttpServerExchange exchange) throws IOException {
        CreateRequest request = readBody(exchange, CreateRequest.class);
        long id = bank.createAccount(request.getName());
        exchange.setStatusCode(StatusCodes.CREATED);
        exchange.getResponseSender().send(String.valueOf(id));
    }

    public void notFoundHandler(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Page Not Found!");
    }


    public void start() {
        httpServer.start();
    }
}
