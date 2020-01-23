package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://106.14.172.108:3306/store_manager");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("TempPass1234_");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
