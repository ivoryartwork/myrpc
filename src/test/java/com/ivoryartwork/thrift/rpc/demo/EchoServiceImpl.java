package com.ivoryartwork.thrift.rpc.demo;

import org.apache.thrift.TException;

//实现类
public class EchoServiceImpl implements EchoService.Iface {

    @Override
    public String echo(String msg) throws TException {
        return "server :" + msg;
    }

    @Override
    public void ping() throws TException {

    }
}
