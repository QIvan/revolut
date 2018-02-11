package com.revolut.interview

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.Logger
import org.slf4j.LoggerFactory


const val ACCOUNT = "/account";
const val INFO = ACCOUNT + "/info";
const val CREATE = ACCOUNT + "/create";
const val REFILL = ACCOUNT + "/refill";
const val TRANSFER = ACCOUNT + "/transfer";
const val ID = "id"

/**
 * @author Ivan Zemlyanskiy
 */
class HttpBankServer(private val bank: Bank, val port: Int) {
    companion object {
        private val log = LoggerFactory.getLogger(HttpBankServer::class.java)
    }


    private val server =
            embeddedServer(Netty, port) {
                install(DefaultHeaders)
                routing {
                    get("$INFO/{$ID}") {
                        val id = call.parameters[ID]?.toLong()!!
                        val account = bank.findAccount(id) ?: run {
                            call.respond(HttpStatusCode.NoContent, "{}")
                            return@get
                        }


                    }

                }
            }

    //
    //    public static final String ACCOUNT = "/account";
    //    public static final String INFO = "/info";
    //    public static final String CREATE = "/create";
    //    public static final String REFILL = "/refill";
    //    public static final String TRANSFER = "/transfer";
    //
    //    private final Undertow httpServer;
    //    private final Bank bank;
    //    private final ObjectMapper jsonMapper = new ObjectMapper();
    //
    //    public HttpBankServer(Bank bank, int portIncrement) {
    //        this.bank = bank;
    ////        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //
    //        HttpConfig httpConfig = ConfigFactory.create(HttpConfig.class);
    //
    //        int httpPort = httpConfig.port() + portIncrement;
    //        httpServer = Undertow.builder()
    //                //we have hazelcast's thread pool already
    //                .setWorkerOption(Options.WORKER_TASK_LIMIT, 10)
    //                .addHttpListener(httpPort, httpConfig.host())
    //                .setHandler(Handlers.exceptionHandler(
    //                        Handlers.path()
    //                                .addPrefixPath(ACCOUNT, Handlers.routing()
    //                                        .get(INFO + "/{id}", exchange -> exchange.dispatch(this::info))
    //                                        .post(CREATE, exchange -> exchange.dispatch(this::create))
    //                                        .post(REFILL, exchange -> exchange.dispatch(this::refill))
    //                                        .post(TRANSFER, exchange -> exchange.dispatch(this::transfer))
    //                                        .setFallbackHandler(this::notFoundHandler)
    //                                )
    //                        ).addExceptionHandler(Throwable.class, exchange -> {
    //                            Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);
    //                            log.error("", throwable);
    //                            exchange.setStatusCode(500);
    //                        })
    //                )
    //                .build();
    //    }
    //
    //    private long extractId(HttpServerExchange exchange) {
    //        Deque<String> idParams = exchange.getQueryParameters().get("id");
    //        return Long.parseLong(idParams.poll());
    //    }
    //
    //    private Optional<Account> findAccount(HttpServerExchange exchange, long id) {
    //        Optional<Account> findResult = bank.findAccount(id);
    //        if (!findResult.isPresent()) {
    //            exchange.setStatusCode(StatusCodes.NO_CONTENT);
    //            exchange.getResponseSender().send("{}");
    //            return Optional.empty();
    //        }
    //        return findResult;
    //    }
    //
    //    private void info(HttpServerExchange exchange) {
    //        long id = extractId(exchange);
    //
    //        findAccount(exchange, id).ifPresent(account -> {
    //            try {
    //                exchange.startBlocking();
    //                jsonMapper.writeValue(exchange.getOutputStream(), account);
    //            } catch (IOException e) {
    //                throw new RuntimeException(e);
    //            }
    //        });
    //    }
    //
    //    private <A> A readBody(HttpServerExchange exchange, Class<A> type) throws IOException {
    //        final A result;
    //        exchange.startBlocking();
    //        try (InputStream inputStream = exchange.getInputStream()) {
    //            result = jsonMapper.readValue(inputStream, type);
    //        }
    //        return result;
    //    }
    //
    //    private void refill(HttpServerExchange exchange) throws IOException {
    //        RefillRequest request = readBody(exchange, RefillRequest.class);
    //        bank.refill(request.getId(), new BigDecimal(request.getAmount()));
    //    }
    //
    //    private void transfer(HttpServerExchange exchange) throws IOException {
    //        TransferRequest request = readBody(exchange, TransferRequest.class);
    //        boolean transferSuccess = bank.transfer(request.getDonorId(),
    //                                                request.getAcceptorId(),
    //                                                new BigDecimal(request.getAmount()));
    //        if (!transferSuccess) {
    //            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
    //        }
    //    }
    //
    //    private void create(HttpServerExchange exchange) throws IOException {
    //        CreateRequest request = readBody(exchange, CreateRequest.class);
    //        long id = bank.createAccount(request.getName());
    //        exchange.setStatusCode(StatusCodes.CREATED);
    //        exchange.getResponseSender().send(String.valueOf(id));
    //    }
    //
    //    public void notFoundHandler(HttpServerExchange exchange) {
    //        exchange.setStatusCode(StatusCodes.NOT_FOUND);
    //        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
    //        exchange.getResponseSender().send("Page Not Found!");
    //    }


    fun start() {
        server.start();
    }
}
