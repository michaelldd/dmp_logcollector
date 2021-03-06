package com.elex.bigdata.service;

import com.elex.bigdata.util.DMUtils;
import com.elex.bigdata.util.YACConstants;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Author: liqiang
 * Date: 14-7-4
 * Time: 上午9:42
 */
public class FormatYACLog implements Callable<String>{
    public static final Log LOG = LogFactory.getLog(FormatYACLog.class);
    public static final Log LOG_AMAZON = LogFactory.getLog("amazon");
    public static final Log LOG_EBAY = LogFactory.getLog("ebay");
    private static Random random = new Random();
    private String zipfilePath;

    public FormatYACLog(String zipfilePath){
        this.zipfilePath = zipfilePath;
    }

    @Override
    public String call() throws Exception {
        FileInputStream fis = null;
        BufferedReader reader = null;

        try{
            LOG.debug("Unzip file " + zipfilePath);
            String filePath = unzipFile(zipfilePath);
            File datFile = new File(filePath);

            if(!datFile.exists()){
                return "null";
            }

            if(datFile.length() == 0){
                LOG.warn("Error " + zipfilePath + " length is zero");
                datFile.delete();
                return "zero";
            }

            //parseFile
            fis = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(fis));

            String firstLine = reader.readLine().replace(YACConstants.YAC_UNICODE, "");
            String line = null;

            while((line =  reader.readLine()) != null){
                if(!YACConstants.YAC_UNICODE.equals(line)){
                    try{
                        line =  line.replace(YACConstants.YAC_UNICODE, "");
                        List<String> attrs = DMUtils.split(line,YACConstants.LOG_ATTR_SEPRATOR);

                        String url = attrs.get(1);
                        if(DMUtils.validateURL(url)){
                            //uid  ip nation ts url title 网站语言 metainfo 停留时间
                            StringBuffer sb = new StringBuffer(firstLine);

                            String time = correctTime(attrs.get(0));
                            if(DMUtils.isAmazonVP(url)){
                                String uid = firstLine.substring(0,firstLine.indexOf(YACConstants.LOG_ATTR_SEPRATOR));
                                LOG_AMAZON.info(uid+YACConstants.LOG_ATTR_SEPRATOR + time + YACConstants.LOG_ATTR_SEPRATOR + url);
                            }

                            if(DMUtils.isEbayVP(url)){
                                String uid = firstLine.substring(0,firstLine.indexOf(YACConstants.LOG_ATTR_SEPRATOR));
                                LOG_EBAY.info(uid+YACConstants.LOG_ATTR_SEPRATOR + time + YACConstants.LOG_ATTR_SEPRATOR + url);
                            }

                            //只取到“?”之前的URL

                            int index = url.indexOf("?") ;
                            if(index > -1){
                                url = url.substring(0,index);
                            }
                            if(url.length() >100){
                                continue;
                            }

                            sb.append(YACConstants.LOG_ATTR_SEPRATOR);
                            sb.append(time).append(YACConstants.LOG_ATTR_SEPRATOR)
                                    .append(url).append(YACConstants.LOG_ATTR_SEPRATOR)
                                    .append(attrs.get(2)).append(YACConstants.LOG_ATTR_SEPRATOR)
                                    .append(attrs.get(3)).append(YACConstants.LOG_ATTR_SEPRATOR)
                                    .append(attrs.get(4)).append(YACConstants.LOG_ATTR_SEPRATOR)
                                    .append(attrs.get(5)).append(YACConstants.LOG_ATTR_SEPRATOR);
                            YACConstants.CONTENT_QUEUE.add(sb.toString());
                        }/*else{
                            if(attrs.get(1).length() < 500){
                                LOG_INVALID.debug(attrs.get(1));
                            }
                        }*/
                    }catch(Exception e){
                        LOG.warn("Error while parse " + line + " : " + e.getMessage());
                    }
                }
            }
//            LOG.debug("Delete file " + filePath);
            new File(filePath).delete();
        }catch (Exception e){
            LOG.warn("Error while process " + zipfilePath + " " + e.getMessage());
            return "fail";
        }finally {
            if(reader != null){
                reader.close();
            }
            if(fis != null){
                fis.close();
            }

        }
        return "ok";
    }

    public String unzipFile(String filePath){
/*        ZipFile zipFile = new ZipFile(filePath);
        String zipFileName = zipFile.getFile().getName();
        String fileName = zipFileName.substring(0,zipFileName.indexOf("."));
        try{
            zipFile.extractAll(YACConstants.unzip_path);
        }catch (Exception e){
            LOG.warn("unzip " + filePath + " " + e.getMessage());
        }*/

        //API解压有问题，直接调用系统unzip

        String fullDatPath = null;
        int retry = 4;
        File zipFile = new File(filePath);
        String fileName = zipFile.getName().substring(0, zipFile.getName().indexOf("."));
        fullDatPath = YACConstants.unzip_path+"/" + fileName + ".dat";
        String shellCommand = "unzip -oqc "+ filePath +" > " + fullDatPath;
        String[] cmd = {"/bin/sh", "-c", shellCommand};

        try {
            Process pid = Runtime.getRuntime().exec(cmd);
            pid.waitFor();
            new File(filePath).delete(); //删除
        } catch (Exception e) {
            LOG.warn("Error while unzip " + zipfilePath + " " + e.getMessage());
        }
        new File(filePath).delete(); //删除
        return fullDatPath;
    }

    public String getTimeSuffix(){
        //时间只有10位，手动加上3位随机数
        int randomTime = random.nextInt(999);
        String timeSuffix = String.valueOf(randomTime);
        if(randomTime == 0){
            timeSuffix = "000";
        }if(randomTime<10){
            timeSuffix = "00" + randomTime;
        }else if(randomTime<100){
            timeSuffix = "0" + randomTime;
        }
        return timeSuffix;
    }

    //yac的时间为用户本机时间，与实际时间差别较大，如果差一天，简单处理为设置为当天
    public String correctTime(String ts){
        long t = Long.parseLong(ts + getTimeSuffix());
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(t);
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int min = time.get(Calendar.MINUTE);
        int sec = time.get(Calendar.SECOND);
        int msec = time.get(Calendar.MILLISECOND);

        time.setTime(new Date());

        if(Math.abs(time.getTimeInMillis() - t) > YACConstants.valid_date_range){
            if(hour < time.get(Calendar.HOUR_OF_DAY)){
                time.set(Calendar.HOUR_OF_DAY, hour);
            }
            time.set(Calendar.MINUTE, min);
            time.set(Calendar.SECOND, sec);
            time.set(Calendar.MILLISECOND, msec);
            t = time.getTimeInMillis();
        }

        return String.valueOf(t);
    }
}
