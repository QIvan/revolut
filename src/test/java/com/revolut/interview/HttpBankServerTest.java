package com.revolut.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.revolut.interview.model.Account;
import io.undertow.util.StatusCodes;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.revolut.interview.HttpBankServerKt.*;
import static org.junit.Assert.*;

/**
 * @author Ivan Zemlyanskiy
 */
@Ignore
public class HttpBankServerTest {

    public static final String ADDRESS = "http://localhost";
    public static final int TRANSFER_COUNT = 5_000;
    public static final int ACCOUNT_INIT_AMOUNT = Integer.MAX_VALUE;
    private static final int ACCOUNTS_NUMBER = 10;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    private int port;
    private final Random random = new Random(System.currentTimeMillis());

    @Before
    public void setUp() throws Exception {
        Bank bank = new Bank();
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        HttpBankServer server = new HttpBankServer(bank, port);
        server.start();
    }

    @Test
    public void concurrentTransferShouldBeSequentiallyConsistent() throws Exception {

        assertEquals(StatusCodes.NO_CONTENT, Unirest.get(testAddress(accountInfoPath(0))).asString().getStatus());

        List<Long> accounts = new ArrayList<>();
        for (int i = 0; i < ACCOUNTS_NUMBER; i++) {
            accounts.add(createAndRefillAccount());
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(100);
        List<Future<HttpResponse<String>>> responses = random.ints(TRANSFER_COUNT, 0, 100)
                .boxed()
                .map(amount -> (Callable<HttpResponse<String>>) () -> {
                    JSONObject jsonObject = new JSONObject();
                    int id = random.nextInt(accounts.size());
                    jsonObject.put("donor", accounts.get(id));
                    jsonObject.put("acceptor", accounts.get((id + 1) % accounts.size()));
                    jsonObject.put("amount", amount);
                    return Unirest.post(testAddress(ACCOUNT + TRANSFER))
                            .body(jsonObject).asString();
                })
                .map(futureCallable -> {
                    try {
                        return threadPool.submit(futureCallable);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        for (Future<HttpResponse<String>> response : responses) {
            assertEquals(StatusCodes.OK, response.get().getStatus());
        }


        // summary of all money is constant
        BigDecimal expectedResult = accounts.stream()
                .map(id -> new BigDecimal(ACCOUNT_INIT_AMOUNT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultAmount = accounts.stream()
                .map(id -> {
                    try {
                        Account accountInfo = jsonMapper.readValue(
                                Unirest.get(testAddress(accountInfoPath(id))).asString().getBody(),
                                Account.class);
                        return accountInfo.getMoney();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(expectedResult, resultAmount);

    }

    private Long createAndRefillAccount() throws UnirestException {
        JSONObject accountName = new JSONObject();
        accountName.put("name", UUID.randomUUID().toString());
        HttpResponse<String> responseCreate = Unirest.post(testAddress(ACCOUNT + CREATE))
                .body(accountName)
                .asString();
        assertEquals(StatusCodes.CREATED, responseCreate.getStatus());
        Long id = Long.valueOf(responseCreate.getBody());

        JSONObject refill = new JSONObject();
        refill.put("id", id);
        refill.put("amount", ACCOUNT_INIT_AMOUNT);
        HttpResponse<String> responseRefill = Unirest.post(testAddress(ACCOUNT + REFILL))
                .body(refill)
                .asString();
        assertEquals(StatusCodes.OK, responseRefill.getStatus());

        return id;
    }

    private String accountInfoPath(long id) {
        return ACCOUNT + INFO + "/" + id;
    }

    private String testAddress(String path) {
        return ADDRESS + ":" + port + path;
    }


}