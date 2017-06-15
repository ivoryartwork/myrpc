package com.ivoryartwork.thrift.rpc.demo;

import org.apache.thrift.TException;

/**
 * Created by Yaochao on 2016/7/1.
 */
public class HelloWorldServiceImpl implements HelloWorldService.Iface {
    @Override
    public String sayHello(String username, String test) throws TException {
        return "Hi," + username + ":" + test;
    }
}
