package com.revolut.interview.config;

import org.aeonbits.owner.Config;

/**
 * @author Ivan Zemlyanskiy
 */
public interface HttpConfig extends Config {

    @DefaultValue("9000")
    int port();

    @DefaultValue("0.0.0.0")
    String host();

}
