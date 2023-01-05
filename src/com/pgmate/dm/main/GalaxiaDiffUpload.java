package com.pgmate.dm.main;

import com.jcraft.jsch.JSch;
import com.pgmate.dm.dao.KsnetDiffUploadDAO;
import com.pgmate.dm.util.KSPGFtsUpDownLib;
import com.pgmate.dm.util.SFTPUtil;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GalaxiaDiffUpload {

	private static Logger logger = LoggerFactory.getLogger( GalaxiaDiffUpload.class );

	private static String HOST = "192.168.95.139";
	private static int PORT = 22;

	private static String UPLOAD_PATH="D:\\galaxia\\upload\\";
	private static String GALAXIA_UPLOAD_PATH="/upload";
	private static String DOWNLOAD_PATH="D:\\galaxia\\download\\";

	private SmsGw smsGw = null;
	private String day = "";

	//22.06.02 vanid 분리용 배열 추가
	private String[] vanId = {"2010000007" , "2010000008" , "2010000010" , "2010000011"};

	public GalaxiaDiffUpload() {
		galaxiaSftp();
	}

	public void galaxiaSftp() {
		final SFTPUtil sftpUtil = new SFTPUtil();

		final String userId = "mysftpuser";
		final String userPw = "1234";

		day = CommonUtil.getCurrentDate("yyyyMMdd");

		//접속
		sftpUtil.init(HOST, userId, userPw, PORT);

		String fileName = UPLOAD_PATH + File.separator + "가맹점AID_RECEIVE_INFO." + day;
//		String fileName = UPLOAD_PATH + "/가맹점AID_RECEIVE_INFO." + day;

		logger.info("upload file ===> {}", fileName);

		//파일 객체 생성
		File uploadFile = new File(fileName);

		try{
			logger.info("1");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile)));
			logger.info("2");
			bw.write("냠 123 Test~~~");
			bw.close();

			if(sftpUtil.upload(GALAXIA_UPLOAD_PATH, uploadFile)){
				logger.info("===== UPLOAD SUCCESSS =====");
			} else {
				logger.info("===== UPLOAD FAIL =====");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		sftpUtil.download(GALAXIA_UPLOAD_PATH, "가맹점AID_RECEIVE_INFO.20230104", DOWNLOAD_PATH+"\\DOWNLOADFILE");
//		File downloadFile = new File(DOWNLOAD_PATH+"\\DOWNLOADFILE");

		sftpUtil.disconnection();
	}

	public void makeDiffMcht() {

		KsnetDiffUploadDAO dao = new KsnetDiffUploadDAO();
		List<SharedMap<String, Object>> mchtList = dao.getMchtList(); 
		//smsGw = new SmsGw();
		
		try {
			String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
			day = nowDate.substring(0,4) + "년 " + nowDate.substring(4,6) + "월 " + nowDate.substring(6) + "일";
			String path = UPLOAD_PATH+nowDate.substring(0,6);
			String fileName = path+File.separator+nowDate+".ksnet.upload.txt";
			File folder = new File(path);
			if(!folder.exists()) {
				folder.mkdir();
			}
			logger.info("diffMcht fileName [{}]",fileName);
			File file = new File(fileName);
//				if(file.exists()) {
//					file.delete();
//				}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"euc-kr"));
			for(SharedMap<String, Object> map:mchtList) {
				bw.write("D,");
				bw.write(map.getString("regType").replace(",", "")+",");
				bw.write(map.getString("compNo").replace(",", "")+",");
				bw.write(map.getString("vanId").replace(",", "")+",");
				bw.write(map.getString("mchtCompNo").replace(",", "")+",");
				bw.write(map.getString("bizType").replace(",", "")+",");
				bw.write(map.getString("mchtName").replace(",", "")+",");
				bw.write(map.getString("mchtUrl").replace(",", "")+",");
				bw.write(map.getString("addr1").replace(",", "")+map.getString("addr2").replace(",", "")+",");
				bw.write(map.getString("mchtCeo").replace(",", "")+",");
				bw.write(map.getString("mchtPhone").replace(",", "")+",");
				bw.write(map.getString("mchtEmail").replace(",", "")+",");
				bw.write(map.getString("filler").replace(",", "")+",");
				bw.newLine();
			}
				
			bw.close();
				
			// KSNET 파일업로드
			if(KSPGFtsUpDownLib.fileUpload(HOST, PORT, fileName, "PGSBM", "2010000007","12", nowDate) < 0) {
				logger.info("DIFF MCHT UPLOAD FAIL!");
			}else{
				logger.info("DIFF MCHT UPLOAD SUCCESS!");
				if(mchtList.size()>0) {
					dao.updateDiffMcht(mchtList);
				}
			}
		}catch (Exception e) {
			String msgBody = day + " KSNET 하위사업자 등록 파일 송신 오류. 확인요망";
			//smsGw.sendMessage("0", "1", msgBody);
            
            logger.error(e.getMessage(), e);
		}
	}
	
	public void makeDiffSettle() {
		String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
		day = nowDate.substring(0,4) + "년 " + nowDate.substring(4,6) + "월 " + nowDate.substring(6) + "일";
		KsnetDiffUploadDAO dao = new KsnetDiffUploadDAO();
		
		for (String id : vanId) {

			List<SharedMap<String,Object>> payList = dao.getPayList(id);
			List<SharedMap<String,Object>> rfdList = dao.getRfdList(id);
			List<SharedMap<String,Object>> errList = dao.getErrList(id);
			try {
				String path = UPLOAD_PATH+nowDate.substring(0,6);
				String fileName = path+File.separator+nowDate+"("+id+")"+".ksnet.upload.txt";
				File folder = new File(path);
				if(!folder.exists()) {
					folder.mkdir();
				}
				File file = new File(fileName);
	//				if(file.exists()) {
	//					file.delete();
	//				}
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"euc-kr"));
				for(SharedMap<String, Object> map:payList) {
					bw.write(map.getString("recordType").trim()+",");
					bw.write(map.getString("systemType").trim()+",");
					bw.write(map.getString("vanId").trim()+",");
					bw.write(map.getString("trxType").trim()+",");
					bw.write(map.getString("trxDay").trim()+",");
					bw.write(map.getString("compNo").trim()+",");
					bw.write(map.getString("mchtCompNo").trim()+",");
					bw.write(map.getString("vanTrxId").trim()+",");
					bw.write(map.getString("rfdTurn").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("trxId").trim()+",");
					bw.write(" ,");
					bw.newLine();
				}
				for(SharedMap<String, Object> map:rfdList) {

					// 부분취소 순번 추가
					if(map.getString("trxType").equals("3")) {
						int cnt = 0;
						//cnt = dao.getRfdListCnt(map.getString("rootTrxId"), map.getString("trxDay") , map.getString("trxTime"));
						map.replace("rfdTurn", Integer.toString(cnt+1));
					}

					bw.write(map.getString("recordType").trim()+",");
					bw.write(map.getString("systemType").trim()+",");
					bw.write(map.getString("vanId").trim()+",");
					bw.write(map.getString("trxType").trim()+",");
					bw.write(map.getString("trxDay").trim()+",");
					bw.write(map.getString("compNo").trim()+",");
					bw.write(map.getString("mchtCompNo").trim()+",");
					bw.write(map.getString("vanTrxId").trim()+",");
					bw.write(map.getString("rfdTurn").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("trxId").trim()+",");
					bw.write(" ,");
					bw.newLine();
				}
				// 22.09.04 차액정산 오류코드 받은 거래건 다시 업로드
				for(SharedMap<String, Object> map:errList) {
					bw.write(map.getString("recordType").trim()+",");
					bw.write(map.getString("systemType").trim()+",");
					bw.write(map.getString("vanId").trim()+",");
					bw.write(map.getString("trxType").trim()+",");
					bw.write(map.getString("trxDay").trim()+",");
					bw.write(map.getString("compNo").trim()+",");
					bw.write(map.getString("mchtCompNo").trim()+",");
					bw.write(map.getString("vanTrxId").trim()+",");
					bw.write(map.getString("rfdTurn").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("amount").trim()+",");
					bw.write(map.getString("trxId").trim()+",");
					bw.write(" ,");
					bw.newLine();
				}
				bw.close();
				
				// KSNET 파일업로드
				if(KSPGFtsUpDownLib.fileUpload(HOST, PORT, fileName, "PGTMS", id ,"12", nowDate) < 0) {
					logger.info("DIFF TRX UPLOAD FAIL!");
				}else {
					logger.info("DIFF TRX UPLOAD SUCCESS!");
					if(payList.size()>0) {
						dao.insertTrxDiffUpload(payList, nowDate);
					}
					if(rfdList.size()>0) {
						dao.insertTrxDiffUpload(rfdList, nowDate);
					}
				}

			}catch (Exception e) {
				String msgBody = day + " KSNET 차액정산 파일 송신 오류. 확인요망";

				smsGw.sendMessage("0", "1", msgBody);

	            logger.error(e.getMessage(), e);
			}
		}
	}
	
	public static void main(String[] args) {
		new GalaxiaDiffUpload();

	}

}
