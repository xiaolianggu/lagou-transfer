package com.lagou.edu.controller;

import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyController;
import com.lagou.edu.annotation.MyRequestMapping;
import com.lagou.edu.pojo.Result;
import com.lagou.edu.service.TransferService;
import com.lagou.edu.utils.JsonUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@MyController
@MyRequestMapping("/test")
public class TransferController {

    @MyAutowired
    TransferService transferService;


    @MyRequestMapping("/transfer")
    public void transfer(HttpServletRequest req, HttpServletResponse resp) throws Exception{


        // 设置请求体的字符编码
        req.setCharacterEncoding("UTF-8");

        String fromCardNo = req.getParameter("fromCardNo");
        String toCardNo = req.getParameter("toCardNo");
        String moneyStr = req.getParameter("money");
        int money = Integer.parseInt(moneyStr);

        Result result = new Result();

        try {

            // 2. 调用service层方法
            transferService.transfer(fromCardNo,toCardNo,money);
            result.setStatus("200");
        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus("201");
            result.setMessage(e.toString());
        }

        // 响应
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().print(JsonUtils.object2Json(result));
    }


}