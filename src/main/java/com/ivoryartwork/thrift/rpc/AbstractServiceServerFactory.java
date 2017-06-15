package com.ivoryartwork.thrift.rpc;

import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerAddressRegister;
import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerIpLocalNetworkResolve;
import com.ivoryartwork.thrift.rpc.zookeeper.ThriftServerIpResolve;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;

/**
 * Created by Yaochao on 2016/6/30.
 */
public abstract class AbstractServiceServerFactory implements Closeable {

    // 服务注册本机端口
    protected Integer port = 8299;// default
    // 优先级
    protected Integer weight = 1;// default

    // 解析本机IP
    protected ThriftServerIpResolve thriftServerIpResolve;
    //服务注册
    protected ThriftServerAddressRegister thriftServerAddressRegister;

    protected ServerThread serverThread;

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setThriftServerIpResolve(ThriftServerIpResolve thriftServerIpResolve) {
        this.thriftServerIpResolve = thriftServerIpResolve;
    }

    public void setThriftServerAddressRegister(ThriftServerAddressRegister thriftServerAddressRegister) {
        this.thriftServerAddressRegister = thriftServerAddressRegister;
    }

    /**
     * 获取服务器ip地址
     *
     * @return
     * @throws Exception
     */
    protected String getServerHostName() throws Exception {
        if (thriftServerIpResolve == null) {
            //如果没有配置，默认从网卡获取
            thriftServerIpResolve = new ThriftServerIpLocalNetworkResolve();
        }
        String serverIP = thriftServerIpResolve.getServerIp();
        if (StringUtils.isEmpty(serverIP)) {
            throw new ThriftException("cant find server ip...");
        }

        String hostname = serverIP + ":" + port + ":" + weight;
        return hostname;
    }

    /**
     * 根据服务创建相应的TProcessor
     *
     * @param service
     * @return
     * @throws IllegalClassFormatException
     */
    protected TProcessor createServiceTProcessor(Object service) throws IllegalClassFormatException {
        Class<?> serviceClass = service.getClass();
        // 获取实现类接口
        Class<?>[] interfaces = serviceClass.getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalClassFormatException("service-class should implements Iface");
        }
        // reflect,load "Processor";
        TProcessor processor = null;
        String serviceName = null;
        for (Class<?> clazz : interfaces) {
            String cname = clazz.getSimpleName();
            if (!cname.equals("Iface")) {
                continue;
            }
            serviceName = clazz.getEnclosingClass().getName();
            String pname = serviceName + "$Processor";
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Class<?> pclass = classLoader.loadClass(pname);
                if (!TProcessor.class.isAssignableFrom(pclass)) {
                    continue;
                }
                Constructor<?> constructor = pclass.getConstructor(clazz);
                processor = (TProcessor) constructor.newInstance(service);
                break;
            } catch (Exception e) {
                //
            }
        }
        if (processor == null) {
            throw new IllegalClassFormatException("service-class should implements Iface");
        }
        return processor;
    }

    class ServerThread extends Thread {
        private TServer server;

        ServerThread(TProcessor processor, int port) throws Exception {
            TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(port);
            TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(serverTransport);
            TProcessorFactory processorFactory = new TProcessorFactory(processor);
            tArgs.processorFactory(processorFactory);
            tArgs.transportFactory(new TFramedTransport.Factory());
            tArgs.protocolFactory(new TBinaryProtocol.Factory(true, true));
            tArgs.maxReadBufferBytes = 1024 * 1024L;
            server = new TThreadedSelectorServer(tArgs);
        }

        @Override
        public void run() {
            try {
                //启动服务
                server.serve();
            } catch (Exception e) {
                //
            }
        }

        public void stopServer() {
            server.stop();
        }
    }

    public void close() {
        serverThread.stopServer();
    }
}
