package com.ivoryartwork.thrift.rpc.demo;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//客户端调用
@SuppressWarnings("resource")
public class Client {
    public static void main(String[] args) {
//        simple();
        spring();
    }

    public static void spring() {
        try {
            final ApplicationContext context = new ClassPathXmlApplicationContext("spring-context-thrift-client.xml");
            ExecutorService service = Executors.newFixedThreadPool(500);
            for (int i = 0; i < 500; i++) {
                service.submit(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                EchoService.Iface echoSerivce = (EchoService.Iface) context.getBean("echoService");
                                System.out.println(echoSerivce.echo("hello--echo"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            service.shutdown();
            service.awaitTermination(2, TimeUnit.HOURS);

//            //关闭连接的钩子
//            Runtime.getRuntime().addShutdownHook(new Thread() {
//                public void run() {
//                    Map<String, ThriftServiceClientProxyFactory>
//                            clientMap = context.getBeansOfType(ThriftServiceClientProxyFactory.class);
//                    for (Entry<String, ThriftServiceClientProxyFactory> client : clientMap.entrySet()) {
//                        System.out.println("serviceName : " + client.getKey() + ",class obj: " + client.getValue());
//                        client.getValue().close();
//                    }
//                }
//            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class TThread extends Thread {
        EchoService.Iface echoSerivce;

        TThread(EchoService.Iface service) {
            echoSerivce = service;
        }

        public void run() {
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(1000 * i);
                    System.out.println(Thread.currentThread().getName() + "  " + echoSerivce.echo("hello"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void simple() {
        try {
            TSocket socket = new TSocket("127.0.0.1", 9000);
            TTransport transport = new TFramedTransport(socket);
            TProtocol protocol = new TBinaryProtocol(transport);
            TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, "co.smys.platform.rpcservice.EchoService");
            co.smys.platform.rpcservice.EchoService.Client client = new co.smys.platform.rpcservice.EchoService.Client(mp);
            transport.open();
            while (true) {
                try {
                    TTransport pin = client.getInputProtocol().getTransport();
                    TTransport pout = client.getOutputProtocol().getTransport();
                    if (pin.isOpen() && pout.isOpen()) {
                        client.echo("helloword");
                        Thread.sleep(1000);
                    } else {
                        System.out.println("close");
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            transport.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
