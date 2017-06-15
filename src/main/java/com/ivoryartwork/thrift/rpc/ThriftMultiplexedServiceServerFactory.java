package com.ivoryartwork.thrift.rpc;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * 服务端多个服务注册工厂
 */
public class ThriftMultiplexedServiceServerFactory extends AbstractServiceServerFactory implements InitializingBean {

    private List<RPCService> rpcServices;

    public void setRpcServices(List<RPCService> rpcServices) {
        this.rpcServices = rpcServices;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (rpcServices.size() == 0) {
            throw new Exception("has no rpcServices");
        }
        //获取服务器的ip地址
        String hostName = getServerHostName();

        TMultiplexedProcessor processor = new TMultiplexedProcessor();
        for (RPCService rpcService : rpcServices) {
            TProcessor p = createServiceTProcessor(rpcService.getService());
            processor.registerProcessor(rpcService.getName(), p);
        }
        //需要单独的线程,因为serve方法是阻塞的.
        serverThread = new ServerThread(processor, port);
        serverThread.start();
        // 注册服务
        for (RPCService rpcService : rpcServices) {
            if (thriftServerAddressRegister != null) {
                thriftServerAddressRegister.register(rpcService.getName(), rpcService.getVersion(), hostName);
            }
        }
    }
}
