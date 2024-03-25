package com.pgmate.dm.main;

import com.pgmate.dm.dao.GalaxiaDiffDownloadDAO;
import com.pgmate.dm.dao.WelcomeDiffDownloadDAO;
import com.pgmate.dm.exception.DiffTransportException;
import com.pgmate.dm.util.SFTPUtil;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WelcomeDiffMchtDownload {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private SmsGw smsGw = null;
    private String msgBody = "";

    // TEST SFTP SERVER
    private static String HOST = "118.130.130.27";
    // LIVE SFTP SERVER
//    private static String HOST = "118.129.171.153";
    private static int PORT = 5555;

    //WELCOME SFTP SERVER
    private static String userId = "bkwinners";
    private static String serverIdentity = "/home/bkwinners/.ssh/id_rsa";

    //    private static String MCHT_PATH="D:\\welcome\\diffMcht\\";
    private static String MCHT_PATH="/home/bkwinners/diff/mcht_welcome/";

    private static String WELCOME_DOWNLOAD_PATH ="/upload/dfsttm/recv";

    private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();

    private int dataCnt = 0;
    private long dataAmt = 0;
    private int rowCnt = 0;

    private String identity = "6758600152";

    public WelcomeDiffMchtDownload(String nowDate) { downloadDiffMcht(nowDate); }

    private void downloadDiffMcht(String nowDate) {
        logger.info("========== WELCOME 하위사업자 결과 등록 START ==========");
        String downloadPath = MCHT_PATH + nowDate.substring(0, 6);
        String fileName = "merc_welcome_" + identity + "_" + nowDate + "_rslt";

        WelcomeDiffDownloadDAO dao = new WelcomeDiffDownloadDAO();

        File folder = new File(downloadPath);
        if(!folder.exists()) {
            folder.mkdir();
        }

        FileInputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        final SFTPUtil sftpUtil = new SFTPUtil();
        try {
            sftpUtil.init(HOST, userId, serverIdentity, PORT);

            logger.info("===== WELCOME 하위사업자 결과 파일 경로 : {} =====", WELCOME_DOWNLOAD_PATH + File.separator + fileName);

            if (sftpUtil.exists(WELCOME_DOWNLOAD_PATH + File.separator + fileName)) {
                logger.info("WELCOME 하위사업자 결과 파일 EXIST");

                downloadPath += File.separator + nowDate + ".welcome.download";
                sftpUtil.download(WELCOME_DOWNLOAD_PATH, fileName, downloadPath);

                File file = new File(downloadPath);

                is = new FileInputStream(file);
                isr = new InputStreamReader(is, "EUC-KR");
                br = new BufferedReader(isr);
                String line = "";

                while ((line = br.readLine()) != null) {
                    logger.info("WELCOME 하위사업자 등록 DATA : [" + line + "]");

                    if (line.startsWith("10")) {
                        parssingHeader(line);
                    } else if (line.startsWith("11")) {
                        parssingData(line);
                    } else if (line.startsWith("30")) {
                        parssingTotal(line);
                    }
                }
            } else {
                logger.info("WELCOME 하위사업자 결과 파일 NOT EXIST");
            }
        } catch (Exception e) {
            logger.error("DOWNLOAD MCHT DIFF ERROR ===> {}", e.getMessage());
            msgBody = "웰컴 영중소 가맹점 다운로드 오류. 확인요망 [" + e.getMessage() + "]";
            smsGw.sendMessage("0", "4", msgBody);
        } finally {
            sftpUtil.disconnection();
            try {
                if(br != null) br.close();
                if(isr != null) isr.close();
                if(is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("===== WELCOME 하위사업자 결과 등록 END =====");
    }

    private void parssingHeader(String data) {
        logger.info("line data check : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String date = CommonUtil.toString(resBuf, 2, 8).trim();
        String filler = CommonUtil.toString(resBuf, 10, 490).trim();
    }

    private void parssingData(String data) {
        WelcomeDiffDownloadDAO dao = new WelcomeDiffDownloadDAO();
        String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String regType = CommonUtil.toString(resBuf, 2, 2).trim();
        String compNo = CommonUtil.toString(resBuf, 4, 10).trim();
        String cardCd = CommonUtil.toString(resBuf, 14, 2).trim();
        String cardName = "";
        switch (cardCd) {
            case "01":cardName = "하나카드"	;break;
            case "03":cardName = "롯데카드"	;break;
            case "04":cardName = "현대카드"	;break;
            case "06":cardName = "국민카드"	;break;
            case "11":cardName = "BC카드"	;break;
            case "12":cardName = "삼성카드"	;break;
            case "14":cardName = "신한카드"	;break;
            case "16":cardName = "농협카드"	;break;
            case "44":cardName = "우리카드"	;break;
            default:cardName="기타";break;
        }
        String mchtCompNo = CommonUtil.toString(resBuf, 16, 10).trim();
        String bizType = CommonUtil.toString(resBuf, 26, 20).trim();
        String mchtName = CommonUtil.toString(resBuf, 46, 40).trim();
        String mchtUrl = CommonUtil.toString(resBuf, 86, 80).trim();
        String addr = CommonUtil.toString(resBuf, 166, 100).trim();
        String zip	= CommonUtil.toString(resBuf,266,6).trim();
        String mchtCeo	= CommonUtil.toString(resBuf,272,40).trim();
        String mchtPhone	= CommonUtil.toString(resBuf,312,11).trim();
        String mchtEmail	= CommonUtil.toString(resBuf,323,40).trim();
        String uploadDay	= CommonUtil.toString(resBuf,363,8).trim();
        String seq	= CommonUtil.toString(resBuf,371,20).trim();
        String failMsg	= CommonUtil.toString(resBuf,391,2).trim();         // 카드사 반송코드
        String regResult	= CommonUtil.toString(resBuf,393,2).trim();     // 웰컴페이먼츠 반송코드
        String filler	= CommonUtil.toString(resBuf,395,105).trim();

        // failMsg(카드사코드)를 상세메시지로 사용
        if(CommonUtil.isNullOrSpace(failMsg)) {
            switch (regResult) {
                case "00":failMsg = "정상처리"	;break;
                case "01":failMsg = "등록구분 오류"	;break;
                case "11":failMsg = "필수값 누락(하위몰)"	;break;
                case "12":failMsg = "필수값 누락(회사명)"	;break;
                case "13":failMsg = "필수값 누락(URL)"	;break;
                case "14":failMsg = "필수값 누락(전화번호)"	;break;
                case "15":failMsg = "필수값 누락(기타)"	;break;
                case "99":failMsg = "기타오류"	;break;
            }
        } else if(failMsg.equals("00")) {
            failMsg = "정상처리";
        } else {
            failMsg = dao.getFailMsg(cardCd, failMsg);
        }

        SharedMap<String, Object> uploadData = dao.getMchtUploadData(mchtCompNo);

        SharedMap<String, Object> map = new SharedMap<String, Object>();
        map.put("recordType", recordType);
        map.put("regType", regType);
        map.put("compNo", compNo);
        map.put("vanId", uploadData.get("vanId"));
        map.put("mchtId", uploadData.get("mchtId"));
        map.put("mchtCompNo", mchtCompNo);
        map.put("uploadDay", uploadDay);
        map.put("cardReqDay", "");
        map.put("resultDay", nowDate);
        map.put("cardCode", cardCd);
        map.put("cardName", cardName);
        map.put("intrsFree", "");
        map.put("regResult", regResult);
        map.put("failMsg", failMsg);
        map.put("filler", filler);
        map.put("regDay", nowDate);

        list.add(map);

        dataCnt++;

        dao.insertMchtDiffDownLoad(list);
    }

    private void parssingTotal(String data) {
        logger.info("MCHT DIFF TAIL LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String downloadDay = CommonUtil.toString(resBuf, 2, 8).trim();
        String filler = CommonUtil.toString(resBuf, 10, 490).trim();
    }

    public static void main(String[] args) throws IOException {
		new WelcomeDiffMchtDownload(args[0]);
//        new WelcomeDiffMchtDownload("20230412");
    }
}
