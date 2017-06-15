package com.ivoryartwork.thrift.rpc;

import java.lang.instrument.IllegalClassFormatException;

/**
 * Created by Yaochao on 2016/7/1.
 */
public class RPCService {

    private Object service;

    private String version;

    public Object getService() {
        return service;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() throws Exception {
        Class<?> serviceClass = service.getClass();
        // 获取实现类接口
        Class<?>[] interfaces = serviceClass.getInterfaces();
        for (Class<?> clazz : interfaces) {
            String cname = clazz.getSimpleName();
            if (!cname.equals("Iface")) {
                continue;
            }
            return clazz.getEnclosingClass().getName();
        }
        throw new IllegalClassFormatException("service-class should implements Iface");
    }
}
