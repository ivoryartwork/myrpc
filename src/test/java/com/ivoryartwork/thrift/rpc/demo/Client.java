package com.ivoryartwork.thrift.rpc.demo;

import com.ivoryartwork.thrift.rpc.ThriftServiceClientProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;
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

            //关闭连接的钩子
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Map<String, ThriftServiceClientProxyFactory>
                            clientMap = context.getBeansOfType(ThriftServiceClientProxyFactory.class);
                    for (Map.Entry<String, ThriftServiceClientProxyFactory> client : clientMap.entrySet()) {
                        System.out.println("serviceName : " + client.getKey() + ",class obj: " + client.getValue());
                        client.getValue().close();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
