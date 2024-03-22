package com.pgmate.dm.main;

import com.pgmate.dm.dao.GalaxiaDiffDownloadDAO;
import com.pgmate.dm.dao.WelcomeDiffDownloadDAO;
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

public class WelcomeDiffTrxDownload {
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

    private static String MCHT_PATH="D:\\welcome\\diffMcht\\";
    private static String SETTLE_PATH="D:\\welcome\\diffSettle\\";

    //    private static String GALAXIA_DOWNLOAD_PATH="/test/";	//테스트 폴더
    private static String WELCOME_DOWNLOAD_PATH="/receive";	//운영 폴더

    private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();

    private int dataCnt = 0;
    private long dataAmt = 0;
    private int rowCnt = 0;

    private String identity = "6758600152";

    public WelcomeDiffTrxDownload(String nowDate) { downloadDiffTrx(nowDate); }

    private void downloadDiffTrx(String nowDate) {
        smsGw = new SmsGw();
        logger.info("========== WELCOME 차액정산 결과 등록 START ==========");
        String downloadPath = SETTLE_PATH + nowDate.substring(0, 6);

        String fileNameList[] = {"daff_welcome_" + identity + "_" + nowDate + "_valid"
                            ,"daff_welcome_" + identity + "_" + nowDate + "_res"};

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
            sftpUtil.initWithIdentity(HOST, userId, serverIdentity, PORT);

            for(String fileName : fileNameList) {

                logger.info("===== WELCOME 차액정산 결과 파일 경로 : {} =====", WELCOME_DOWNLOAD_PATH + File.separator + fileName);
                if (sftpUtil.exists(WELCOME_DOWNLOAD_PATH + "/" + fileName)) {
                    logger.info("WELCOME 차액정산 결과 파일 EXIST");

                    downloadPath += File.separator + nowDate + ".welcome.download";
                    sftpUtil.download(WELCOME_DOWNLOAD_PATH, fileName, downloadPath);

                    File file = new File(downloadPath);

                    is = new FileInputStream(file);
                    isr = new InputStreamReader(is, "EUC-KR");
                    br = new BufferedReader(isr);

                    String line = "";

                    String fileType = fileName.substring(fileName.lastIndexOf("_") + 1);
                    while ((line = br.readLine()) != null) {
                        logger.info("WELCOME 차액정산 등록 DATA : [" + line + "]");

                        if (line.startsWith("10")) {
                            parssingHeader(line);
                        } else if (line.startsWith("11")) {
                            parssingData(line, fileType);
                        } else if (line.startsWith("12")) {
                            parssingTotal(line, fileType);
                        }
                    }
                    if (dao.updateTrxDiff(list, fileType) > 0) {
                        logger.info("updateTrxCap [{}]", dao.updateTrxCap(nowDate, fileType));
                    }
                } else {
                    logger.info("GALAXIA 차액정산 결과 파일 NOT EXIST");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("WELCOME 차액정산 다운로드 차액예정일자 ERROR");
        }
    }

    private void parssingHeader(String data) {
        logger.info("TRX DIFF HEADER LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String date = CommonUtil.toString(resBuf, 2, 8).trim();
        String compNo = CommonUtil.toString(resBuf, 10, 10).trim();
        String pg = CommonUtil.toString(resBuf, 20, 10).trim();
        String filler = CommonUtil.toString(resBuf, 30, 320).trim();
    }

    private void parssingData(String data, String fileType) {
        logger.info("TRX DIFF BODY LINE DATA : {}", data);
        GalaxiaDiffDownloadDAO dao = new GalaxiaDiffDownloadDAO();
        String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String trxType = CommonUtil.toString(resBuf, 2, 1).trim();
        String trxDay = CommonUtil.toString(resBuf, 3, 8).trim();
        String compNo = CommonUtil.toString(resBuf, 11, 10).trim();
        String mchtCompNo = CommonUtil.toString(resBuf, 21, 10).trim();
        String vanTrxId = CommonUtil.toString(resBuf, 31, 40).trim();
        String rootVanTrxId = CommonUtil.toString(resBuf, 71, 40).trim();
        String trxId = CommonUtil.toString(resBuf, 111, 64).trim();
        String amount = CommonUtil.toString(resBuf, 175, 15).trim();
        String rootAmount = CommonUtil.toString(resBuf, 190, 15).trim();
        String rootTrxDay = CommonUtil.toString(resBuf, 205, 8).trim();
        String mchtFiller = CommonUtil.toString(resBuf, 213, 40).trim();

        if(fileType.equals("res")) {
            String mchtCode = CommonUtil.toString(resBuf, 253, 1).trim();
            String mchtType = "";
            switch (mchtCode) {
                case "0":mchtType = "영세";break;
                case "1":mchtType = "중소1";break;
                case "2":mchtType = "중소2";break;
                case "3":mchtType = "중소3";break;
                case "4":mchtType = "일반";break;
            }
            String capType = CommonUtil.toString(resBuf, 254, 1).trim();
            String cardType = CommonUtil.toString(resBuf, 255, 1).trim();
            String stlDiffStlAmt = CommonUtil.toString(resBuf, 256, 15).trim();
            String stlDiffStlAmtVat = CommonUtil.toString(resBuf, 271, 15).trim();

            Long diffStlAmt = 0L;
            Long diffStlAmtVat = 0L;

            //차액정산금이 양수 일 때 그대로 반영
            if(!stlDiffStlAmt.contains("-")) {
                diffStlAmt = Long.valueOf(stlDiffStlAmt);
                diffStlAmtVat = Long.valueOf(diffStlAmtVat);
                //차액정산금이 음수 일 때 '-' 부호 앞의 '0'들 제거 후 반영
            } else {
                stlDiffStlAmt = stlDiffStlAmt.substring(stlDiffStlAmt.indexOf("-"));
                diffStlAmt = Long.valueOf(stlDiffStlAmt);
                stlDiffStlAmtVat = stlDiffStlAmtVat.substring(stlDiffStlAmtVat.indexOf("-"));
                diffStlAmt = Long.valueOf(stlDiffStlAmtVat);
            }

            String diffStlDay = CommonUtil.toString(resBuf, 286, 8).trim();
            String resultCd = CommonUtil.toString(resBuf, 294, 2).trim();
            String resultType = CommonUtil.toString(resBuf, 296, 1).trim();
            String filler = CommonUtil.toString(resBuf, 296, 54).trim();

            SharedMap<String, Object> payMap = dao.getDiffUploadData(vanTrxId);
            SharedMap<String, Object> map = new SharedMap<String, Object>();
            map.put("recordType", "R");
            map.put("trxId", trxId);
            map.put("resultCd", resultCd);
            map.put("mchtType", mchtType);
            map.put("mchtCode", mchtCode);
            map.put("cardType", cardType);
            map.put("diffStlAmt", diffStlAmt);
            map.put("diffStlDay", diffStlDay);
            map.put("downDay", nowDate);

            list.add(map);
        } else {
            String resultCd = CommonUtil.toString(resBuf, 253, 2).trim();
            SharedMap<String, Object> map = new SharedMap<String, Object>();
            map.put("recordType", "R");
            map.put("trxId", trxId);
            map.put("resultCd", resultCd);
            map.put("downDay", nowDate);

            list.add(map);
        }

    }

    private void parssingTotal(String data, String fileType) {
        logger.info("TRX DIFF TAIL LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String totCnt = CommonUtil.toString(resBuf, 2, 7).trim();
        String totAmt = CommonUtil.toString(resBuf, 9, 18).trim();
        String totDiffAmt = CommonUtil.toString(resBuf, 27, 15).trim();
        String filler = CommonUtil.toString(resBuf, 42, 158).trim();
    }

    public static void main(String[] args) throws IOException {
        new WelcomeDiffTrxDownload(args[0]);
//        new WelcomeDiffTrxDownload("20240326");
    }
}
