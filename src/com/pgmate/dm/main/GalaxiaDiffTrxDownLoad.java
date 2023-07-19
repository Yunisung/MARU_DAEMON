package com.pgmate.dm.main;

import com.pgmate.dm.dao.GalaxiaDiffDownloadDAO;
import com.pgmate.dm.util.SFTPUtil;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GalaxiaDiffTrxDownLoad {
    private static Logger logger = LoggerFactory.getLogger( GalaxiaDiffTrxDownLoad.class );

    private SmsGw smsGw = null;
    private String msgBody = "";
    //GALAXIA SFTP SERVER
    private static String HOST = "119.207.70.214";
    private static int PORT = 22;

    //GALAXIA SFTP SERVER
    private static String userId = "A2240732";
    private static String userPw = "1!qnrnrdnlsjtm0732";

    //BK 서버 업로드 파일 저장 경로
    private static String SETTLE_PATH="/home/bkwinners/diff/settle_galaxia/";
//    private static String SETTLE_PATH="D:\\galaxia\\diffSettle\\";

//    private static String GALAXIA_DOWNLOAD_PATH="/test/";	//테스트 폴더
	private static String GALAXIA_DOWNLOAD_PATH="/receive";	//운영 폴더

    private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();

    private String day = "";

    private int dataCnt = 0;
    public GalaxiaDiffTrxDownLoad(String nowDate) {
        downloadDiffTrx(nowDate);
    }

    private void downloadDiffTrx(String nowDate) {
        smsGw = new SmsGw();
        logger.info("========== GALAXIA 차액정산 결과 등록 START ==========");
        String downloadPath = SETTLE_PATH + nowDate.substring(0, 6);
        String fileName = userId + "_RECEIVE." + nowDate;

        GalaxiaDiffDownloadDAO dao = new GalaxiaDiffDownloadDAO();

        File folder = new File(downloadPath);
        if(!folder.exists()) {
            folder.mkdir();
        }

        try {
            final SFTPUtil sftpUtil = new SFTPUtil();

            sftpUtil.init(HOST, userId, userPw, PORT);

            logger.info("===== GALAXIA 차액정산 결과 파일 경로 : {} =====", GALAXIA_DOWNLOAD_PATH + File.separator + fileName);
            if(sftpUtil.exists(GALAXIA_DOWNLOAD_PATH + "/" + fileName)) {
                logger.info("GALAXIA 차액정산 결과 파일 EXIST");

                downloadPath += File.separator + nowDate + ".galaxia.download";
                sftpUtil.download(GALAXIA_DOWNLOAD_PATH, fileName, downloadPath);

                File file = new File(downloadPath);

                FileInputStream is = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(is, "EUC-KR");
                BufferedReader br = new BufferedReader(isr);
                String line = "";

                while ((line = br.readLine()) != null) {
                    logger.info("GALAXIA 차액정산 등록 DATA : [" + line + "]");

                    if (line.startsWith("HD")) {
                        parssingHeader(line);
                    } else if (line.startsWith("DT")) {
                        parssingData(line);
                    } else if (line.startsWith("TR")) {
                        parssingTotal(line);
                    }
                }
//                if(dao.updateTrxDiff(list) > 0) {
//                    logger.info("updateTrxCap [{}]",dao.updateTrxCap(nowDate));
//                }
                br.close();
            }else {
                logger.info("GALAXIA 차액정산 결과 파일 NOT EXIST");
            }

            sftpUtil.disconnection();

        } catch (Exception e) {
            logger.error("DOWNLOAD TRX DIFF ERROR ===> {}", e.getMessage());
            msgBody = "갤럭시아 차액정산 다운로드 오류. 확인요망 [" + e.getMessage() + "]";
            smsGw.sendMessage("0", "4", msgBody);
        }

        logger.info("===== GALAXIA 차액정산 결과 등록 END =====");
    }



    private void parssingHeader(String data) {
        logger.info("TRX DIFF HEADER LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String date = CommonUtil.toString(resBuf, 2, 8).trim();
        String aid = CommonUtil.toString(resBuf, 10, 20).trim();
        String filler = CommonUtil.toString(resBuf, 30, 170).trim();
    }

    private void parssingData(String data) {
        logger.info("TRX DIFF BODY LINE DATA : {}", data);
        GalaxiaDiffDownloadDAO dao = new GalaxiaDiffDownloadDAO();
        String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String trxType = CommonUtil.toString(resBuf, 2, 1).trim();
        String trxDay = CommonUtil.toString(resBuf, 3, 8).trim();
        String compNo = CommonUtil.toString(resBuf, 11, 10).trim();
        String mchtCompNo = CommonUtil.toString(resBuf, 21, 10).trim();
        String vanTrxId = CommonUtil.toString(resBuf, 31, 20).trim();
        String trxCnt = CommonUtil.toString(resBuf, 51, 2).trim();
        String trxId = CommonUtil.toString(resBuf, 53, 64).trim();
        String mchtSalesAmt = CommonUtil.toString(resBuf, 117, 15).trim();
        String amount = CommonUtil.toString(resBuf, 132, 15).trim();
        String udf = CommonUtil.toString(resBuf, 147, 30).trim();
        String mchtType = "";
        String mchtCode = CommonUtil.toString(resBuf, 177, 1).trim();
        switch (mchtCode) {
            case "0":mchtType = "영세";break;
            case "1":mchtType = "중소1";break;
            case "2":mchtType = "중소2";break;
            case "3":mchtType = "중소3";break;
            case "4":mchtType = "일반";break;
        }

        String stlDiffStlAmt = CommonUtil.toString(resBuf, 178, 15).trim();
        Long diffStlAmt = 0L;

        //차액정산금이 양수 일 때 그대로 반영
        if(!stlDiffStlAmt.contains("-")) {
            diffStlAmt = Long.valueOf(stlDiffStlAmt);
        //차액정산금이 음수 일 때 '-' 부호 앞의 '0'들 제거 후 반영
        } else {
            stlDiffStlAmt = stlDiffStlAmt.substring(stlDiffStlAmt.indexOf("-"));
            diffStlAmt = Long.valueOf(stlDiffStlAmt);
        }

        String resultCd = CommonUtil.toString(resBuf, 193, 2).trim();
        String mchtFiller = CommonUtil.toString(resBuf, 195, 5).trim();

        SharedMap<String, Object> payMap = dao.getDiffUploadData(vanTrxId);
        SharedMap<String, Object> map = new SharedMap<String, Object>();
        map.put("recordType", "R");
        map.put("trxId", trxId);
        map.put("resultCd", resultCd);
        map.put("mchtType", mchtType);
        map.put("mchtCode", mchtCode);
        String cardType = payMap.getString("cardType");
        switch (cardType) {
            case "신용" : map.put("cardType","0");break;
            case "체크" : map.put("cardType","1");break;
        }
        map.put("diffStlAmt", diffStlAmt);
        String diffStlDay = dao.getSettleDay(getNextDay(CommonUtil.getCurrentDate("yyyyMMdd")));
        map.put("diffStlDay", diffStlDay);
        map.put("downDay", nowDate);

        list.add(map);
    }

    private void parssingTotal(String data) {
        logger.info("TRX DIFF TAIL LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String totCnt = CommonUtil.toString(resBuf, 2, 7).trim();
        String totAmt = CommonUtil.toString(resBuf, 9, 18).trim();
        String totDiffAmt = CommonUtil.toString(resBuf, 27, 15).trim();
        String filler = CommonUtil.toString(resBuf, 42, 158).trim();
    }

    private String getNextDay(String nowDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            //다음 날짜 구하기
            Date date = sdf.parse(nowDate);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            calendar.add(Calendar.DATE, 1);

            return sdf.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("GALAXIA 차액정산 다운로드 차액예정일자 ERROR");
            return nowDate;
        }
    }

    public static void main(String[] args) {
        new GalaxiaDiffTrxDownLoad("20230412");
    }
}
