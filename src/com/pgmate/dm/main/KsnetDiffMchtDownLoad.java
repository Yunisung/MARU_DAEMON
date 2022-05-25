package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KsnetDiffDownloadDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.dm.util.KSPGFtsUpDownLib;
import com.pgmate.lib.util.map.SharedMap;

public class KsnetDiffMchtDownLoad {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KsnetDiffMchtDownLoad.class );

//	private static String HOST = "210.181.28.119";	//KSNET 개발
	private static String HOST = "210.181.28.137";	//KSNET 운영
	private static int PORT = 9800;	//KSNET 서버포트

	private static String MCHT_PATH="/home/data/diff/mcht/";

//	private static String MCHT_PATH="C:\\dev\\test\\mcht\\";
	
	private static String ENC_SHOP_PASS = "TSX8cRHaPu4R3i2VWjG/Pg==";
	
	private SmsGw smsGw = null;
	private String day = "";
	
	public KsnetDiffMchtDownLoad(String nowDate) {
		downLoadDiffMcht(nowDate);
	}
	
	public void downLoadDiffMcht(String nowDate) {
		DecimalFormat formatter = new DecimalFormat("###,###");
		smsGw = new SmsGw();
		day = nowDate.substring(0,4) + "년 " + nowDate.substring(4,6) + "월 " + nowDate.substring(6) + "일";
		
		try {
			String path = MCHT_PATH+nowDate.substring(0, 6);
			String fileName = path+File.separator+nowDate+".ksnet.download.txt";
			KsnetDiffDownloadDAO dao = new KsnetDiffDownloadDAO();
			
			File folder = new File(path);
			if(!folder.exists()) {
				folder.mkdir();
			}
			
			if(KSPGFtsUpDownLib.fileDownload(HOST, PORT, fileName, "PGSBM", "0", "1006500000",ENC_SHOP_PASS, nowDate) > -1) {
				List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
				
				try {
					
					File cvs = new File(fileName);
					FileReader fr = new FileReader(cvs);
					BufferedReader br = new BufferedReader(fr);
					String line = "";
					
					while((line = br.readLine()) != null) {
						String [] token = line.split(",",-1);
						SharedMap<String, Object> map = new SharedMap<String, Object>();
						map.put("recordType", token[0]);
						map.put("regType", token[1]);
						map.put("compNo", token[2]);
						map.put("vanId", token[3]);
						map.put("mchtCompNo", token[4]);
						map.put("uploadDay", token[5]);
						map.put("cardReqDay", token[6]);
						map.put("resultDay", token[7]);
						map.put("cardCode", token[8]);
						String cardName = "";
						switch (token[8]) {
							case "010001":cardName = "BC카드"	;break;
							case "010002":cardName = "KB카드"	;break;
							case "010003":cardName = "KEB하나카드"	;break;
							case "010004":cardName = "삼성카드";break;
							case "010005":cardName = "신한카드";break;
							case "010008":cardName = "현대카드";break;
							case "010009":cardName = "롯데카드";break;
							case "010015":cardName = "NH카드"	;break;
							default:cardName="기타";break;
						}
						map.put("cardName", cardName);
						map.put("intrsFree", token[9]);
						map.put("regResult", token[10]);
						map.put("failMsg", token[11]);
						map.put("filler", token[12]);
						map.put("regDay", nowDate);
						
						map.put("mchtId", dao.getMchtId(map));
						
						list.add(map);
					}
					br.close();
				}catch (Exception e) {
					logger.error(e.getMessage());
				}
				dao.insertMchtDiffDownLoad(list);
				
				String msgBody = day + " KSNET 하위사업자 " + formatter.format(list.size()) + "건 등록 완료.";
				smsGw.sendMessage("0", "2", msgBody);
			}else {
				String msgBody = day + " KSNET 하위사업자 다운로드 파일이 존재하지 않습니다.";
				smsGw.sendMessage("0", "2", msgBody);
			}
		} catch(Exception e) {
			String msgBody = day + " KSNET 하위사업자 등록 오류. 확인요망";
			smsGw.sendMessage("0", "2", msgBody);
            
            logger.error(e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) {
		new KsnetDiffMchtDownLoad(args[0]);
	}

}
