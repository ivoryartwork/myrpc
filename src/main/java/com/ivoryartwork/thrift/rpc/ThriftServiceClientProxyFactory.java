package com.ivoryartwork.thrift.rpc;

import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 客户端代理
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ThriftServiceClientProxyFactory implements FactoryBean, InitializingBean, Closeable {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Integer maxTotal = 100;// 最大活跃连接数

    //是否开启TMultiplexed 协议
    private boolean multiplexedProtocol = false;

    // ms,default 3 min,链接空闲时间
    // -1,关闭空闲检测
    private Integer idleTime = 180000;
    private ThriftServerAddressProvider serverAddressProvider;

    private Object proxyClient;
    private Class<?> objectClass;

    private GenericObjectPool<TServiceClient> pool;

    public AbstractThriftClientPoolFactory.PoolOperationCallBack callback = new AbstractThriftClientPoolFactory.PoolOperationCallBack() {
        @Override
        public void create(TServiceClient client) {
            logger.info("create");
        }

        @Override
        public void destroy(TServiceClient client) {
            logger.info("destroy");
        }
    };

    public void setMaxTotal(Integer maxTotal) {
        this.maxTotal = maxTotal;
    }

    public void setIdleTime(Integer idleTime) {
        this.idleTime = idleTime;
    }

    public void setServerAddressProvider(ThriftServerAddressProvider serverAddressProvider) {
        this.serverAddressProvider = serverAddressProvider;
    }

    public void setMultiplexedProtocol(boolean multiplexedProtocol) {
        this.multiplexedProtocol = multiplexedProtocol;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 加载Iface接口
        objectClass = classLoader.loadClass(serverAddressProvider.getService() + "$Iface");
        // 加载Client.Factory类
        Class<TServiceClientFactory<TServiceClient>> fi = (Class<TServiceClientFactory<TServiceClient>>) classLoader.loadClass(serverAddressProvider.getService() + "$Client$Factory");
        TServiceClientFactory<TServiceClient> clientFactory = fi.newInstance();
        PooledObjectFactory clientPool = null;
        if (multiplexedProtocol) {
            clientPool = new ThriftMultiplexedClientPoolFactory(serverAddressProvider, clientFactory, callback);
        } else {
            clientPool = new ThriftClientPoolFactory(serverAddressProvider, clientFactory, callback);
        }
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(0);
        poolConfig.setMinEvictableIdleTimeMillis(idleTime);
        poolConfig.setTimeBetweenEvictionRunsMillis(idleTime * 2L);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(false);
        pool = new GenericObjectPool<TServiceClient>(clientPool, poolConfig, abandonedConfig);
        proxyClient = Proxy.newProxyInstance(classLoader, new Class[]{objectClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //
                TServiceClient client = pool.borrowObject();
                boolean flag = true;
                try {
                    return method.invoke(client, args);
                } catch (Exception e) {
                    flag = false;
                    throw e;
                } finally {
                    if (flag) {
                        pool.returnObject(client);
                    } else {
                        pool.invalidateObject(client);
                    }
                }
            }
        });
    }

    @Override
    public Object getObject() throws Exception {
        return proxyClient;
    }

    @Override
    public Class<?> getObjectType() {
        return objectClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void close() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (serverAddressProvider != null) {
            try {
                serverAddressProvider.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
