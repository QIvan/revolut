package com.revolut.interview.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.ToString;
import lombok.Value;

/**
 * @author Ivan Zemlyanskiy
 */
@Value
@ToString
public class TransferRequest {

    @JsonAlias("donor")
    long donorId;
    @JsonAlias("acceptor")
    long acceptorId;
    String amount;

}
