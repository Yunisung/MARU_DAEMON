package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KsnetDiffDownloadDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.dm.util.KSPGFtsUpDownLib;
import com.pgmate.lib.util.map.SharedMap;

public class KsnetDiffTrxDownLoad {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KsnetDiffTrxDownLoad.class );

//	private static String HOST = "210.181.28.119";	//KSNET 개발
	private static String HOST = "210.181.28.137";	//KSNET 운영
	private static int PORT = 9800;	//KSNET 서버포트

	private static String SETTLE_PATH="/home/data/diff/settle/";
//	private static String SETTLE_PATH="D:\\ksnet\\diffSettle\\";

	private static String ENC_SHOP_PASS = "ec4wxx1foTcnTLpjkFL23Q==";
	private static String ENC_SHOP_PASS_13 = "0RdUH3zaXnYHFhKrKsr+sQ==";

	private SmsGw smsGw = null;
	private String day = "";
	
	//22.06.02 vanid 분리용 배열 추가
	private String[] vanId = {"2010000007" , "2010000008" , "2010000010" , "2010000011", "2010000001", "2010000013"};

	public KsnetDiffTrxDownLoad(String nowDate) {
		downLoadDiffTrx(nowDate);
	}
	
	public void downLoadDiffTrx(String nowDate) {
		DecimalFormat formatter = new DecimalFormat("###,###");
		smsGw = new SmsGw();
		day = nowDate.substring(0,4) + "년 " + nowDate.substring(4,6) + "월 " + nowDate.substring(6) + "일";
		
		for (String id : vanId) {

			try {
				String path = SETTLE_PATH+nowDate.substring(0, 6);
				String fileName = path+File.separator+nowDate+"("+id+")"+".ksnet.download.txt";
				KsnetDiffDownloadDAO dao = new KsnetDiffDownloadDAO();

				if(dao.checkDownSettle(nowDate, id) > 0) {
					logger.info("============= "+nowDate+"일자 차액정산 PASS ===============");
					return;
				}

				File folder = new File(path);
				if(!folder.exists()) {
					folder.mkdir();
				}
				String pass = ENC_SHOP_PASS;
				// KSNET 파일업로드
				if(id.equals("2010000013")){
					pass = ENC_SHOP_PASS_13;
				}
				logger.info("============= "+nowDate+"일자 차액정산 다운로드 시작 ===============");
				if(KSPGFtsUpDownLib.fileDownload(HOST, PORT, fileName, "PGTMS", "0", id, pass, nowDate) > -1) {
					List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
					try {
						File cvs = new File(fileName);
//					File cvs = new File("C:\\dev\\test\\log\\20190929.ksnet.download.txt");
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cvs),"euc-kr"));

						String line = "";

						while((line = br.readLine()) != null) {
							String [] token = line.split(",",-1);
							SharedMap<String, Object> map = new SharedMap<String, Object>();
							map.put("recordType", token[0]);
							map.put("systemType", token[1]);
							map.put("vanId", token[2]);
							map.put("trxType", token[3]);
							map.put("trxDay", token[4]);
							map.put("compNo", token[5]);
							map.put("mchtCompNo", token[6]);
							map.put("vanTrxId", token[7]);
							map.put("rfdTurn", token[8]);
							map.put("mchtSalesAmt", token[9]);
							map.put("amount", token[10]);
							map.put("trxId", token[11]);
							map.put("udf", token[12]);
							map.put("resultCd", token[13]);

							String mchtType = "";
							String mchtCode = token[14];
							switch(mchtCode) {
							case "0":mchtType = "영세";break;
							case "1":mchtType = "중소1";break;
							case "2":mchtType = "중소2";break;
							case "3":mchtType = "중소3";break;
							case "4":mchtType = "일반";break;
							}
							map.put("mchtType", mchtType);
							map.put("mchtCode", mchtCode);
							map.put("cardType", token[15]);
							map.put("diffStlAmt", token[16]);
							map.put("diffStlDay", token[17]);
							map.put("downDay", nowDate);
							list.add(map);
						}
						br.close();
					}catch (Exception e) {
						logger.error(e.getMessage());
					}
					List<SharedMap<String, Object>> errList = dao.getErrList();
					logger.info("errList size : {}", errList.size());
					if(errList != null) {
						logger.info("updateTrxCap errList [{}]", dao.updateErrTrxCap(errList, nowDate));
					}
					if(dao.updateTrxDiff(list) > 0) {
						logger.info("updateTrxCap [{}]",dao.updateTrxCap(nowDate));
					}

					//String msgBody = day + " KSNET 차액정산"+ "("+id+") " + formatter.format(list.size()) + "건 완료.";
					//smsGw.sendMessage("0", "3", msgBody);
				} else {
					String msgBody = day + " KSNET 차액정산 다운로드 파일이 존재하지 않습니다." + "("+id+")";
					smsGw.sendMessage("0", "3", msgBody);
				}

				logger.info("============= "+nowDate+"일자 차액정산 종료 ===============");
			}catch (Exception e) {
				String msgBody = day + " KSNET 차액정산 오류. 확인요망"+"("+id+")";
				smsGw.sendMessage("0", "3", msgBody);

				logger.error(e.getMessage(), e);
			}
		}

	}
	
	public static void main(String[] args) {
		new KsnetDiffTrxDownLoad(args[0]);
	}
}
