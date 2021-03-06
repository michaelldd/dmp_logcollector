package com.elex.bigdata.servlet;

import com.elex.bigdata.service.CheckYacZipFile;
import com.elex.bigdata.service.CombineYacFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Author: liqiang
 * Date: 14-4-3
 * Time: 下午6:06
 */
public class InitServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        System.out.println("----init----");

        Thread combineYac = new Thread(new CombineYacFile());
        combineYac.start();
        Thread checkYac = new Thread(new CheckYacZipFile());
        checkYac.start();
    }
}
