package com.ivoryartwork.thrift.rpc.demo;

import org.apache.commons.pool.impl.GenericObjectPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Yaochao
 * @version 1.0
 */
public class Pool {

    public static void main(String[] args) throws Exception {

        PoolObjectFactory poolObjectFactory = new PoolObjectFactory();
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.maxActive = 32;
        poolConfig.maxIdle = 1;
        poolConfig.minIdle = 0;
        poolConfig.minEvictableIdleTimeMillis = 180000;
        poolConfig.timeBetweenEvictionRunsMillis = 180000 * 2L;
        poolConfig.testOnBorrow = true;
        poolConfig.testOnReturn = false;
        poolConfig.testWhileIdle = false;
        final GenericObjectPool pool = new GenericObjectPool(poolObjectFactory);
        final Set<String> test = new ConcurrentSkipListSet<>();
        ExecutorService service = Executors.newFixedThreadPool(100);
        for(int i=0;i<100;i++){
            service.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Object o = pool.borrowObject();
                            test.add(o.toString());
                            System.out.println(test.size());
                            pool.returnObject(o);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        service.shutdown();
        service.awaitTermination(2, TimeUnit.HOURS);
    }
}
