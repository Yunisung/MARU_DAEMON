package com.pgmate.dm.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KsnetDiffUploadDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.dm.util.KSPGFtsUpDownLib;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class KsnetDiffUpload {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KsnetDiffUpload.class );

//	private static String HOST = "210.181.28.119";	//KSNET 개발
	private static String HOST = "210.181.28.137";	//KSNET 운영
	private static int PORT = 9800;	//KSNET 서버포트
	
	private static String MCHT_PATH="/home/data/diff/mcht/";
	private static String SETTLE_PATH="/home/data/diff/settle/";
//	private static String MCHT_PATH="D:\\dev\\test\\mcht\\";
//	private static String SETTLE_PATH="D:\\dev\\test\\settle\\";

	private static String ENC_SHOP_PASS = "ec4wxx1foTcnTLpjkFL23Q==";
	private static String ENC_SHOP_PASS_13 = "0RdUH3zaXnYHFhKrKsr+sQ==";

	private SmsGw smsGw = null;
	private String day = "";
	
	//22.06.02 vanid 분리용 배열 추가
	private String[] vanId = {"2010000007" , "2010000008" , "2010000010" , "2010000011", "2010000001", "2010000013"};

	public KsnetDiffUpload() {
		makeDiffMcht();
		makeDiffSettle();
	}
	
	public void makeDiffMcht() {
		KsnetDiffUploadDAO dao = new KsnetDiffUploadDAO();
		List<SharedMap<String, Object>> mchtList = dao.getMchtList(); 
		smsGw = new SmsGw();
		
		try {
			String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
			day = nowDate.substring(0,4) + "년 " + nowDate.substring(4,6) + "월 " + nowDate.substring(6) + "일";
			String path = MCHT_PATH+nowDate.substring(0,6);
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
			if(KSPGFtsUpDownLib.fileUpload(HOST, PORT, fileName, "PGSBM", "2010000007",ENC_SHOP_PASS, nowDate) < 0) {
				logger.info("DIFF MCHT UPLOAD FAIL!");
			}else{
				logger.info("DIFF MCHT UPLOAD SUCCESS!");
				if(mchtList.size()>0) {
					dao.updateDiffMcht(mchtList);
				}
			}
		}catch (Exception e) {
			String msgBody = day + " KSNET 하위사업자 등록 파일 송신 오류. 확인요망";
			smsGw.sendMessage("0", "1", msgBody);
            
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
				String path = SETTLE_PATH+nowDate.substring(0,6);
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

				String pass = ENC_SHOP_PASS;
				// KSNET 파일업로드
				if(id.equals("2010000013")){
					pass = ENC_SHOP_PASS_13;
				}
				if(KSPGFtsUpDownLib.fileUpload(HOST, PORT, fileName, "PGTMS", id ,pass, nowDate) < 0) {
					logger.error("DIFF TRX UPLOAD FAIL!");

					String msgBody = day + " KSNET 차액정산 파일 송신 오류. 확인요망";
					smsGw.sendMessage("0", "1", msgBody);
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
				String msgBody = day + " KSNET 차액정산 파일 생성 오류. 확인요망";

				smsGw.sendMessage("0", "1", msgBody);

	            logger.error(e.getMessage(), e);
			}
		}
	}
	
	public static void main(String[] args) {
		new KsnetDiffUpload();
	
	}

}
