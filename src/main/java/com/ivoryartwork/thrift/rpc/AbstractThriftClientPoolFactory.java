package com.ivoryartwork.thrift.rpc;

import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 连接池,thrift-client for spring
 */
public abstract class AbstractThriftClientPoolFactory extends BasePoolableObjectFactory<TServiceClient> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected final ThriftServerAddressProvider serverAddressProvider;
    protected final TServiceClientFactory<TServiceClient> clientFactory;
    protected PoolOperationCallBack callback;

    protected AbstractThriftClientPoolFactory(ThriftServerAddressProvider addressProvider, TServiceClientFactory<TServiceClient> clientFactory) throws Exception {
        this.serverAddressProvider = addressProvider;
        this.clientFactory = clientFactory;
    }

    protected AbstractThriftClientPoolFactory(ThriftServerAddressProvider addressProvider, TServiceClientFactory<TServiceClient> clientFactory,
                                              PoolOperationCallBack callback) throws Exception {
        this.serverAddressProvider = addressProvider;
        this.clientFactory = clientFactory;
        this.callback = callback;
    }

    static interface PoolOperationCallBack {
        // 销毁client之前执行
        void destroy(TServiceClient client);

        // 创建成功是执行
        void make(TServiceClient client);
    }

    @Override
    public void destroyObject(TServiceClient client) throws Exception {
        if (callback != null) {
            try {
                callback.destroy(client);
            } catch (Exception e) {
                logger.warn("destroyObject:{}", e);
            }
        }
        logger.info("destroyObject:{}", client);
        TTransport pin = client.getInputProtocol().getTransport();
        pin.close();
        TTransport pout = client.getOutputProtocol().getTransport();
        pout.close();
    }

    @Override
    public void activateObject(TServiceClient client) throws Exception {
    }

    @Override
    public void passivateObject(TServiceClient client) throws Exception {
    }

    @Override
    public boolean validateObject(TServiceClient client) {
        try {
            Method method = client.getClass().getMethod("ping");
            try {
                method.invoke(client);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }
}
