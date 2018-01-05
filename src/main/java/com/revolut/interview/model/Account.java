package com.revolut.interview.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Ivan Zemlyanskiy
 */
@Data
@EqualsAndHashCode
@ToString
public class Account implements Serializable {
    private final long id;
    private final String name;
    private BigDecimal money = BigDecimal.ZERO;


}
