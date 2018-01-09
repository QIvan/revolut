package com.revolut.interview;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;


public class Starter {

    public static void main(final String[] args) {
        Config cfg = new Config();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        Bank bank = new Bank(instance);

        HttpBankServer server = new HttpBankServer(
                bank,
                (cfg.getNetworkConfig().getPort() - NetworkConfig.DEFAULT_PORT));
        server.start();
    }


}
