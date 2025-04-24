package com.mirth.connect.server.launcher;

import com.mirth.connect.interfaces.IMirth;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Kiran Ayyagari (kiran@sereen.io)
 */
public class EmbeddedEngine { //  implements IMirth
    private IMirth mirth;

    public EmbeddedEngine(Thread mirth) {
        this.mirth = (IMirth) Proxy.newProxyInstance(mirth.getContextClassLoader(), new Class[]{IMirth.class}, new ProxyInvocator(mirth));
    }

    static class ProxyInvocator implements InvocationHandler {
        private Object obj;

        public ProxyInvocator(Object obj) {
            this.obj = obj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try{
                return method.invoke(obj, args);
            }
            catch(Exception e) {
                throw e;
            }
        }
    }

//    public <T> T createProxy(Class<T> cls) {
//        Proxy.newProxyInstance(classLoader, new Class[]{cls}, );
//    }

//    public Object getConfigController() throws Exception {
//        Field cc = oieThread.getClass().getDeclaredField("configurationController");
//        cc.setAccessible(true);
//        ProxyInvocator pi = new ProxyInvocator(cc.get(oieThread));
//        IController ci = (IController);;
//        return ci;
//    }

    //@Override
    public void shutdown() {
        mirth.shutdown();
    }
}
