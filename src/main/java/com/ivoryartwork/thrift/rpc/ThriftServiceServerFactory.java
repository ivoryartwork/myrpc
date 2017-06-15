package com.ivoryartwork.thrift.rpc;

import org.apache.thrift.TProcessor;
import org.springframework.beans.factory.InitializingBean;

/**
 * 服务端单个服务注册工厂
 */
public class ThriftServiceServerFactory extends AbstractServiceServerFactory implements InitializingBean {

    private RPCService rpcService;

    public void setRpcService(RPCService rpcService) {
        this.rpcService = rpcService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (rpcService == null) {
            throw new NullPointerException("rpcService is null");
        }
        //获取服务器的ip地址
        String hostName = getServerHostName();
        TProcessor processor = null;
        processor = createServiceTProcessor(rpcService.getService());
        //需要单独的线程,因为serve方法是阻塞的.
        serverThread = new ServerThread(processor, port);
        serverThread.start();
        // 注册服务
        if (thriftServerAddressRegister != null) {
            thriftServerAddressRegister.register(rpcService.getName(), rpcService.getVersion(), hostName);
        }
    }
}
