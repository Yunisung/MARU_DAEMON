package com.pgmate.dm.main;

import com.pgmate.dm.dao.GalaxiaDiffDownloadDAO;
import com.pgmate.dm.dao.KsnetDiffDownloadDAO;
import com.pgmate.dm.util.SFTPUtil;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GalaxiaDiffMchtDownLoad {
    private static Logger logger = LoggerFactory.getLogger( GalaxiaDiffMchtDownLoad.class );

    private SmsGw smsGw = null;
    private String msgBody = "";
    //GALAXIA SFTP SERVER
    private static String HOST = "119.207.70.214";
    private static int PORT = 22;

    //GALAXIA SFTP SERVER
    private static String userId = "A2240732";
    private static String userPw = "1!qnrnrdnlsjtm0732";

    //BK 서버 업로드 파일 저장 경로
    private static String MCHT_PATH="/home/bkwinners/diff/mcht_galaxia/";
//    private static String MCHT_PATH="D:\\galaxia\\diffMcht\\";

//    private static String GALAXIA_DOWNLOAD_PATH="/test/";	//테스트 폴더
	private static String GALAXIA_DOWNLOAD_PATH="/receive";	//운영 폴더

    private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();

    private String day = "";

    private int dataCnt = 0;

    public GalaxiaDiffMchtDownLoad(String nowDate) {
        downloadDiffMcht(nowDate);
    }

    private void downloadDiffMcht(String nowDate) {
        smsGw = new SmsGw();
        logger.info("========== GALAXIA 하위사업자 결과 등록 START ==========");
        String downloadPath = MCHT_PATH + nowDate.substring(0, 6);
        String fileName = userId + "_RECEIVE_INFO." + nowDate;

        GalaxiaDiffDownloadDAO dao = new GalaxiaDiffDownloadDAO();

        File folder = new File(downloadPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        FileInputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        final SFTPUtil sftpUtil = new SFTPUtil();
        try {

            sftpUtil.init(HOST, userId, userPw, PORT);

            logger.info("===== GALAXIA 하위사업자 결과 파일 경로 : {} =====", GALAXIA_DOWNLOAD_PATH + File.separator + fileName);

            if (sftpUtil.exists(GALAXIA_DOWNLOAD_PATH + File.separator + fileName)) {
                logger.info("GALAXIA 하위사업자 결과 파일 EXIST");

                downloadPath += File.separator + nowDate + ".galaxia.download";
                sftpUtil.download(GALAXIA_DOWNLOAD_PATH, fileName, downloadPath);

                File file = new File(downloadPath);

                is = new FileInputStream(file);
                isr = new InputStreamReader(is, "EUC-KR");
                br = new BufferedReader(isr);
                String line = "";

                while ((line = br.readLine()) != null) {
                    logger.info("GALAXIA 하위사업자 등록 DATA : [" + line + "]");

                    if (line.startsWith("HD")) {
                        parssingHeader(line);
                    } else if (line.startsWith("RD")) {
                        parssingData(line);
                    } else if (line.startsWith("TR")) {
                        parssingTotal(line);
                    }
                }
            } else {
                logger.info("GALAXIA 하위사업자 결과 파일 NOT EXIST");
            }
        } catch (Exception e) {
            logger.error("DOWNLOAD MCHT DIFF ERROR ===> {}", e.getMessage());
            msgBody = "갤럭시아 영중소 가맹점 다운로드 오류. 확인요망 [" + e.getMessage() + "]";
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

        logger.info("===== GALAXIA 하위사업자 결과 등록 END =====");
    }


    private void parssingHeader(String data) {
        logger.info("line data check : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String date = CommonUtil.toString(resBuf, 2, 8).trim();
        String filler = CommonUtil.toString(resBuf, 10, 490).trim();
    }

    private void parssingData(String data) {
        GalaxiaDiffDownloadDAO dao = new GalaxiaDiffDownloadDAO();
        String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String regType = CommonUtil.toString(resBuf, 2, 2).trim();
        String compNo = CommonUtil.toString(resBuf, 4, 10).trim();
        String cardCd = CommonUtil.toString(resBuf, 14, 3).trim();
        String cardName = "";
        switch (cardCd) {
            case "099":cardName = "전체카드"	;break;
            case "051":cardName = "하나카드"	;break;
            case "052":cardName = "BC카드"	;break;
            case "053":cardName = "신한카드"	;break;
            case "050":cardName = "국민카드"	;break;
            case "054":cardName = "삼성카드"	;break;
            case "055":cardName = "롯데카드"	;break;
            case "078":cardName = "농협카드"	;break;
            case "073":cardName = "현대카드"	;break;
            default:cardName="기타";break;
        }
        String mchtCompNo = CommonUtil.toString(resBuf, 17, 10).trim();
        String bizType = CommonUtil.toString(resBuf, 27, 20).trim();
        String mchtName = CommonUtil.toString(resBuf, 47, 40).trim();
        String zip = CommonUtil.toString(resBuf, 87, 5).trim();
        String addr = CommonUtil.toString(resBuf, 93, 100).trim();
        String mchtCeo	= CommonUtil.toString(resBuf,193,40).trim();
        String mchtPhone1	= CommonUtil.toString(resBuf,233,3).trim();
        String mchtPhone2	= CommonUtil.toString(resBuf,236,4).trim();
        String mchtPhone3	= CommonUtil.toString(resBuf,240,4).trim();
        String mchtEmail	= CommonUtil.toString(resBuf,244,40).trim();
        String mchtUrl	= CommonUtil.toString(resBuf,284,80).trim();
        String uploadDay	= CommonUtil.toString(resBuf,364,8).trim();
        String seq	= CommonUtil.toString(resBuf,372,12).trim();
        String failMsg	= CommonUtil.toString(resBuf,384,2).trim();
        String regResult	= CommonUtil.toString(resBuf,386,2).trim();
        String filler	= CommonUtil.toString(resBuf,388,112).trim();

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

//        dao.insertMchtDiffDownLoad(list);
    }

    private void parssingTotal(String data) {
        logger.info("MCHT DIFF TAIL LINE DATA : {}", data);
        byte[] resBuf = data.getBytes(StandardCharsets.UTF_8);

        String recordType = CommonUtil.toString(resBuf, 0, 2).trim();
        String totCnt = CommonUtil.toString(resBuf, 2, 10).trim();
        String totNewCnt = CommonUtil.toString(resBuf, 12, 10).trim();
        String totModCnt = CommonUtil.toString(resBuf, 22, 10).trim();
        String totDelCnt = CommonUtil.toString(resBuf, 32, 10).trim();
        String filler = CommonUtil.toString(resBuf, 24, 458).trim();
    }

    public static void main(String[] args) throws IOException {
//		new KsnetDiffMchtDownLoad(args[0]);
		new GalaxiaDiffMchtDownLoad("20230412");
    }

}
