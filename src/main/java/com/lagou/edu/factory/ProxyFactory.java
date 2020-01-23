package com.lagou.edu.factory;

import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyService;
import com.lagou.edu.annotation.MyTransactional;
import com.lagou.edu.pojo.Account;
import com.lagou.edu.utils.TransactionManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author 应癫
 *
 *
 * 代理对象工厂：生成代理对象的
 */

public class ProxyFactory {


    /*public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }*/

    public ProxyFactory(){

    }

    private static ProxyFactory proxyFactory = new ProxyFactory();

    public static ProxyFactory getInstance() {
        return proxyFactory;
    }



    /**
     * Jdk动态代理
     * @param obj  委托对象
     * @return   代理对象
     */
    public Object getJdkProxy(Object obj) {

        // 获取代理对象
        return  Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;
                        MyTransactional myTransactional =  method.getAnnotation(MyTransactional.class);
                        try{
                            // 开启事务(关闭事务的自动提交)
                            if(myTransactional != null){
                                TransactionManager.getInstance().beginTransaction();
                            }

                            result = method.invoke(obj,args);

                            // 提交事务
                            if(myTransactional != null){
                                TransactionManager.getInstance().commit();
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                            // 回滚事务
                            if(myTransactional != null){
                                TransactionManager.getInstance().rollback();
                            }

                            // 抛出异常便于上层servlet捕获
                            throw e;

                        }

                        return result;
                    }
                });

    }


    /**
     * 使用cglib动态代理生成代理对象
     * @param obj 委托对象
     * @return
     */
    public Object getCglibProxy(Object obj) {
        return  Enhancer.create(obj.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                Object result = null;
                MyTransactional myTransactional =  method.getAnnotation(MyTransactional.class);
                try{
                    // 开启事务(关闭事务的自动提交)
                    if(myTransactional != null){
                        TransactionManager.getInstance().beginTransaction();
                    }


                    result = method.invoke(obj,objects);

                    // 提交事务
                    if(myTransactional != null){
                        TransactionManager.getInstance().commit();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    // 回滚事务
                    if(myTransactional != null){
                        TransactionManager.getInstance().rollback();
                    }
                    // 抛出异常便于上层servlet捕获
                    throw e;

                }
                return result;
            }
        });
    }
}
