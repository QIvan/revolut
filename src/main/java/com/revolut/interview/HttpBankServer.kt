package com.revolut.interview

import com.fasterxml.jackson.databind.SerializationFeature
import com.revolut.interview.model.request.*
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.locations.*
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import org.slf4j.LoggerFactory


const val ACCOUNT = "/accounts";
const val CREATE = "/create";
const val REFILL = "/refill";
const val TRANSFER = "/transfer";

@Location(ACCOUNT + "/{id}") class Info(val id: Long)
@Location(ACCOUNT + CREATE) class Create
@Location(ACCOUNT + "/{id}" +  REFILL) class Refill(val id: Long)
@Location(ACCOUNT + "/{id}" + TRANSFER) class Transfer(val id: Long)

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
                install(CallLogging)
                install(Locations)
                install(ContentNegotiation) {
                    jackson {
                        configure(SerializationFeature.INDENT_OUTPUT, true)
                    }
                }
                install(Routing) {


                    get<Info> { info ->
                        val account = bank.findAccount(info.id) ?: run {
                            call.respond(HttpStatusCode.NoContent, "{}")
                            return@get
                        }

                        call.respond(account)
                    }


                    post<Create>{
                        val body = call.receiveOrNull(CreateRequest::class) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val account = bank.createAccount(body.name)
                        call.respond(HttpStatusCode.Created, account)
                    }


                    post<Refill>{ query ->
                        val body = call.receiveOrNull(RefillRequest::class) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val account = bank.refill(query.id, body.amount)
                        if (account != null) {
                            call.respond(HttpStatusCode.OK, account)
                        } else {
                            call.respond(HttpStatusCode.NotModified)
                        }
                    }


                    post<Transfer>{ query ->
                        val body = call.receiveOrNull(TransferRequest::class) ?: run {
                            call.respond(HttpStatusCode.BadRequest)
                            return@post
                        }

                        val transferSuccess = bank.transfer(query.id, body.acceptorId, body.amount)
                        if (transferSuccess) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotModified)
                        }
                    }


                    get("/{...}") {
                        call.respondRedirect("/")
                    }
                    get("/") {
                        call.respondHtml {
                            head {
                                title { +"Revolut" }
                            }
                            body {
                                h1 { +"Sample application as an interview task." }
                                pre { +"A kotlin html builder rules!" }
                            }
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
        server.start(wait = true);
    }
}
