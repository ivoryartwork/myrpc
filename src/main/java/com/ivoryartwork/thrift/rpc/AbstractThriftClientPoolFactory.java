package com.ivoryartwork.thrift.rpc;

import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接池,thrift-client for spring
 */
public abstract class AbstractThriftClientPoolFactory extends BasePooledObjectFactory<TServiceClient> {

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

    interface PoolOperationCallBack {
        // 销毁client之前执行
        void destroy(TServiceClient client);

        // 创建成功是执行
        void create(TServiceClient client);
    }

    @Override
    public void destroyObject(PooledObject<TServiceClient> p) throws Exception {
        TServiceClient client = p.getObject();
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
        super.destroyObject(p);
    }

    @Override
    public boolean validateObject(PooledObject<TServiceClient> p) {
        TServiceClient client = p.getObject();
        TTransport pin = client.getInputProtocol().getTransport();
        logger.info("validateObject input:{}", pin.isOpen());
        TTransport pout = client.getOutputProtocol().getTransport();
        logger.info("validateObject output:{}", pout.isOpen());
        return pin.isOpen() && pout.isOpen();
    }
}
