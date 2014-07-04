package com.elex.bigdata.service;

import com.elex.bigdata.util.YACConstants;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 循环获取队列中的数据写入到文件中
 * 暂时以50条数据作为文件的分隔依据，暂时忽略重启等可能数据丢失情况
 * Author: liqiang
 * Date: 14-7-4
 * Time: 上午10:50
 */
public class CombineYacFile implements Runnable {

    private static int maxLineNum = 5000000;
    private static SimpleDateFormat day_sdf = new SimpleDateFormat("yyyyMMdd");
    private static SimpleDateFormat sec_sdf = new SimpleDateFormat("hhmmss");

    private FileOutputStream out = null;
    private OutputStreamWriter writer = null;
    private BufferedWriter bw = null;

    @Override
    public void run() {
        int num = 0;
        setWriter();
        while(true){
            String line = YACConstants.CONTENT_QUEUE.poll();
            if(line != null){
                try{
                    bw.write(line);
                    bw.newLine();
                    num++;
                }catch (Exception e){
                }

                if(num == maxLineNum ){
                    setWriter();
                    num = 0;
                }
            }else{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getFilePath(){
        Date date = new Date();
        return YACConstants.log_dir_path + day_sdf.format(date) + "/" + sec_sdf.format(date) + ".log";
    }

    public void setWriter(){

        closeWriter();

        File file = new File(getFilePath());
        if(file == null || !file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if( !file.isDirectory()){
            file.mkdirs();
        }
        writer = new OutputStreamWriter(out);
        bw = new BufferedWriter(writer);
    }

    public void closeWriter(){
        if(bw != null){
            try{
                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}