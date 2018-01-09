package com.revolut.interview.model.request;

import lombok.ToString;
import lombok.Value;

/**
 * @author Ivan Zemlyanskiy
 */
@Value
@ToString
public class RefillRequest {

    long id;
    String amount;

}
