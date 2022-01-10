package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KcpDiffDownloadDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class KcpDiffMchtDownLoad {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KcpDiffMchtDownLoad.class );

	private static String MCHT_PATH="/home/data/diff/kcp/mcht/";

//	private static String MCHT_PATH="C:\\dev\\test\\mcht\\";
//	private static String SETTLE_PATH="C:\\dev\\test\\settle\\";
	private static String uploadPath = "../bin/mcht_download.sh";

	private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
	
	public KcpDiffMchtDownLoad(String nowDate) {
		downLoadDiffMcht(nowDate);
	}
	
	
	public void downLoadDiffMcht(String nowDate) {
		logger.info("============= "+nowDate+"일자 하위사업자 등록 시작  ===============");
		
		boolean fileCheck = false;
		String path = MCHT_PATH + nowDate;
		
		try {
			KcpDiffDownloadDAO dao = new KcpDiffDownloadDAO();
			
			File folder = new File(path);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			
			downloadFtp(uploadPath);
			
			File file = new File(path);
			File fileList[] = file.listFiles();
			
			for(int i = 0; i < fileList.length; i++) {
				logger.info("KCP 하위사업자 등록 Download File List : [" + fileList[i].getName() + "]");
				
				if(fileList[i].isFile()) {
					if(fileList[i].getName().startsWith("KCP_BCFS_FILE_SR00_OUT")) {
						fileCheck = true;
						File cvs = new File(path + fileList[i].getName());
						FileReader fr = new FileReader(cvs);
						BufferedReader br = new BufferedReader(fr);
						String line = "";
						
						while((line = br.readLine()) != null) {
							logger.info("KCP 하위사업자 등록 Data : [" + line + "]");
							
							if(line.startsWith("HD")) {
								parssingHeader(line);
							}else if(line.startsWith("RD")) {
								parssingBody(line);
							}else if(line.startsWith("TR")) {
								parssingTail(line);
							}
						}
						br.close();
						
						logger.info("KCP하위사업자 등록 데이터 개수 : [" + list.size() + "]");
						
						if(list.size() > 0) {
							int insertCnt = dao.insertMchtDiffDownLoad(list);
							
							logger.info("KCP 하위사업자 등록 Inert Count : [" + insertCnt + "]");
						}
					}
				}
			}
			if(!fileCheck) {
				logger.info("하위사업자 등록 결과파일이 존재하지 않습니다!!");
			}
			
			logger.info("============= "+nowDate+"일자 하위사업자 등록 종료  ===============");
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void parssingHeader(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, resBuf.length, resBuf, 0, resBuf.length);
		
		String recordType	= CommonUtil.toString(resBuf,0,2).trim();
		String date	= CommonUtil.toString(resBuf,2,8).trim();
		String filler	= CommonUtil.toString(resBuf,10,490).trim();
	}
	
	private void parssingBody(String data) {
		KcpDiffDownloadDAO dao = new KcpDiffDownloadDAO();
		String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType	= CommonUtil.toString(resBuf,0,2).trim();
		String regType	= CommonUtil.toString(resBuf,2,2).trim();
		String compNo	= CommonUtil.toString(resBuf,4,10).trim(); 
		String cardCd	= CommonUtil.toString(resBuf,14,3).trim();
		String cardName = "";
		switch (cardCd) {
			case "006":cardName = "KEB하나카드"	;break;
			case "016":cardName = "KB카드"	;break;
			case "018":cardName = "NH카드"	;break;
			case "026":cardName = "BC카드"	;break;
			case "027":cardName = "현대카드"	;break;
			case "029":cardName = "신한카드"	;break;
			case "031":cardName = "삼성카드"	;break;
			case "047":cardName = "롯데카드"	;break;
			default:cardName="기타";break;
		}
		
		String mchtCompNo	= CommonUtil.toString(resBuf,17,10).trim();
		
		String bizType	= CommonUtil.toString(resBuf,27,20).trim();
		String mchtName	= CommonUtil.toString(resBuf,47,40).trim();
		String zip	= CommonUtil.toString(resBuf,87,6).trim();
		String addr	= CommonUtil.toString(resBuf,93,100).trim();
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
		
		SharedMap<String, Object> uploadData = dao.getDiffUploadData(seq);
		
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
	}
	
	private void parssingTail(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType = CommonUtil.toString(resBuf,0,2).trim();
		String totCnt = CommonUtil.toString(resBuf,2,10).trim();
		String totNewCnt = CommonUtil.toString(resBuf,12,10).trim();
		String totModCnt = CommonUtil.toString(resBuf,22,10).trim();
		String totDelCnt = CommonUtil.toString(resBuf,32,10).trim();
		String filler = CommonUtil.toString(resBuf,24,458).trim();
	}
	
	private boolean downloadFtp(String ftpPath) {
		boolean ftpUpload = true;
		
		try {
			logger.info("KCP Diff Mcht FTP Download Start!!");
			
			Runtime runtime = Runtime.getRuntime();
	        Process process;
			process = runtime.exec(ftpPath);
		
	        InputStream is = process.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	
	        String line;
	
	        while((line = br.readLine()) != null) {
	        	logger.info("KCP Diff Mcht FTP Download : " + line);
	        }
	        
	        logger.info("KCP Diff Mcht FTP Download END!!");
		} catch (IOException e) {
			ftpUpload = false;
			logger.info("KCP Diff Mcht FTP Download FAIL!!");
			logger.error(e.getMessage(), e);
		}
		return ftpUpload;
	}
	
	public static void main(String[] args) {
		new KcpDiffMchtDownLoad(args[0]);
	}

}
