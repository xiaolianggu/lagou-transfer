package com.lagou.edu.servlet;

import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyController;
import com.lagou.edu.annotation.MyRequestMapping;
import com.lagou.edu.annotation.MyService;
import com.lagou.edu.factory.BeanFactory;
import com.lagou.edu.factory.ProxyFactory;
import com.lagou.edu.service.impl.TransferServiceImpl;
import com.lagou.edu.utils.JsonUtils;
import com.lagou.edu.pojo.Result;
import com.lagou.edu.service.TransferService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author 应癫
 */
//@WebServlet(name="transferServlet",urlPatterns = "/transferServlet")
public class TransferServlet extends HttpServlet {
    /**
     * 属性配置文件
     */
    private Properties contextConfig = new Properties();

    private List<String> classNameList = new ArrayList<>();

    /**
     * IOC 容器 三级缓存
     */
    Map<String, Object> iocMapThree = new HashMap<String, Object>();

    /**
     * IOC 容器 二级缓存
     */
    Map<String, Object> iocMapTwo = new HashMap<String, Object>();

    /**
     * IOC 容器 一级缓存
     */
    Map<String, Object> iocMapOne = new HashMap<String, Object>();

    Map<String, Method> handlerMapping = new HashMap<String, Method>();
    /**
     * 标记对象是否正在创建，防止递归死循环
     */
    Map<String, Integer> isCreated = new HashMap<String, Integer>();
    // 1. 实例化service层对象
    //private TransferService transferService = new TransferServiceImpl();
    //private TransferService transferService = (TransferService) BeanFactory.getBean("transferService");

    // 从工厂获取委托对象（委托对象是增强了事务控制的功能）

    // 首先从BeanFactory获取到proxyFactory代理工厂的实例化对象
    //private ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");
    //private TransferService transferService = (TransferService) proxyFactory.getJdkProxy(BeanFactory.getBean("transferService")) ;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6、运行阶段
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        //1、加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));
        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scan-package"));
        //3、初始化 IOC 容器，将所有相关的类实例保存到 IOC 容器中
        doInstance();
        //4、依赖注入
        doAutowired();
        //5、初始化 HandlerMapping
        initHandlerMapping();
    }


    /**
     * 1、加载配置文件
     *
     */
    private void doLoadConfig(String contextConfigLocation) {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            // 保存在内存
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 7、运行阶段，进行拦截，匹配
     *
     * @param req  请求
     * @param resp 响应
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            try {
                resp.getWriter().write("404 NOT FOUND!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Method method = this.handlerMapping.get(url);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        // 第一个参数是获取方法，后面是参数，多个参数直接加，按顺序对应
        method.invoke(iocMapThree.get(beanName), req, resp);

    }

    /**
     * 4、依赖注入
     */
    private void doAutowired() {
        if (iocMapThree.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMapThree.entrySet()) {
            doAutowired(entry.getKey(),entry.getValue());
        }
    }

    private void doAutowired(String key,Object object) {
        isCreated.put(key,1);
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(MyAutowired.class)) {
                continue;
            }
            System.out.println("[INFO-4] Existence myAutowired.");
            MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
            String beanName = myAutowired.value().trim();
            if ("".equals(beanName)) {
                beanName = field.getType().getSimpleName();
            }
            field.setAccessible(true);
            beanName = toLowerFirstCase(beanName);
            Object obj = getBean(beanName);
            if(isCreated.get(beanName)==null){
                //doAutowired(beanName,obj);
            }
            try {
                field.set(object, ProxyFactory.getInstance().getCglibProxy(obj));//生成代理
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    private Object getBean(String beanName){
        Object obj = iocMapThree.get(beanName);
        if(obj == null){
            obj = iocMapTwo.get(beanName);
            if(obj == null){
                obj = iocMapOne.get(beanName);
            }
        }
        return obj;
    }

    private void removeThreeCache(String beanName){

    }
    /**
     * 3、初始化 IOC 容器，将所有相关的类实例保存到 IOC 容器中
     */
    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }
        try {
            for (String className : classNameList) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();

                    // 保存在 ioc 容器
                    iocMapThree.put(beanName, instance);

                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    MyService myService = clazz.getAnnotation(MyService.class);
                    if (!"".equals(myService.value())) {
                        beanName = myService.value();
                    }
                    Object instance = clazz.newInstance();
                    iocMapThree.put(beanName, instance);
                    // 找类的接口
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMapThree.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist.");
                        }
                        iocMapThree.put(toLowerFirstCase(i.getSimpleName()), instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    /**
     * 扫描相关的类
     */
    private void doScanner(String scanPackage) {
        URL resourcePath = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        if (resourcePath == null) {
            return;
        }
        File classPath = new File(resourcePath.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                // 子目录递归
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = (scanPackage + "." + file.getName()).replace(".class", "");
                // 保存在内容
                classNameList.add(className);
            }
        }
    }

    private void initHandlerMapping() {
        if (iocMapThree.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMapThree.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + "/" + myRequestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
            }
        }

    }


}
