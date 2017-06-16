package com.ivoryartwork.thrift.rpc.demo;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;

import java.util.Random;
import java.util.UUID;

/**
 * @author Yaochao
 * @version 1.0
 */
public class PoolObjectFactory extends BasePoolableObjectFactory<String> {
    @Override
    public String makeObject() throws Exception {

        String s= UUID.randomUUID().toString();
        System.out.println(s);
        return s;
    }
}
