package com.ivoryartwork.thrift.rpc;

import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerAddressProvider;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.net.InetSocketAddress;

/**
 * 连接池,thrift-client for spring
 */
public class ThriftMultiplexedClientPoolFactory extends AbstractThriftClientPoolFactory {

    protected ThriftMultiplexedClientPoolFactory(ThriftServerAddressProvider addressProvider, TServiceClientFactory<TServiceClient> clientFactory) throws Exception {
        super(addressProvider, clientFactory);
    }

    protected ThriftMultiplexedClientPoolFactory(ThriftServerAddressProvider addressProvider, TServiceClientFactory<TServiceClient> clientFactory, PoolOperationCallBack callback) throws Exception {
        super(addressProvider, clientFactory, callback);
    }

    @Override
    public TServiceClient makeObject() throws Exception {
        InetSocketAddress address = serverAddressProvider.selector();
        if (address == null) {
            throw new ThriftException("No provider available for remote service");
        }
        TSocket tsocket = new TSocket(address.getHostName(), address.getPort());
        TTransport transport = new TFramedTransport(tsocket);
        TProtocol protocol = new TBinaryProtocol(transport);
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, serverAddressProvider.getService());
        TServiceClient client = this.clientFactory.getClient(mp);
        transport.open();
        if (callback != null) {
            try {
                callback.make(client);
            } catch (Exception e) {
                logger.warn("makeObject:{}", e);
            }
        }
        return client;
    }

}
