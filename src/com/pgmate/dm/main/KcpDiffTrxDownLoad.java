package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KsnetDiffDownloadDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class KcpDiffTrxDownLoad {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KcpDiffMchtDownLoad.class );

	private static String SETTLE_PATH="/home/data/diff/kcp/settle/";
//	private static String MCHT_PATH="C:\\dev\\test\\mcht\\";
//	private static String SETTLE_PATH="C:\\dev\\test\\settle\\";
	private static String uploadPath = "../bin/settle_download";
	
	private String siteCd = "T0000";
	private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
	
	public KcpDiffTrxDownLoad(String nowDate) {
		downLoadDiffTrx(nowDate);
	}
	
	public void downLoadDiffTrx(String nowDate) {
		logger.info("============= "+nowDate+"일자 차액정산 시작  ===============");
		
		boolean fileCheck = false;
		String path = SETTLE_PATH + nowDate;
		
		try {
			KsnetDiffDownloadDAO dao = new KsnetDiffDownloadDAO();
			
			File folder = new File(path);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			
			downloadFtp(uploadPath);

			File file = new File(path);
			File fileList[] = file.listFiles();
			
			for(int i = 0; i < fileList.length; i++) {
				logger.info("KCP 차액정산 Download File List : [" + fileList[i].getName() + "]");
				
				if(fileList[i].isFile()) {
					if(fileList[i].getName().startsWith("KCP_BCFS_FILE_DF00_OUT")) {
						fileCheck = true;
						File cvs = new File(path + fileList[i].getName());
						FileReader fr = new FileReader(cvs);
						BufferedReader br = new BufferedReader(fr);
						String line = "";
						
						while((line = br.readLine()) != null) {
							logger.info("KCP 차액정산 Data : [" + line + "]");
							
							if(line.startsWith("01")) {
								parssingStart(line);
							}if(line.startsWith("10")) {
								parssingHeader(line);
							}else if(line.startsWith("11")) {
								parssingBody(line);
							}else if(line.startsWith("12")) {
								parssingTotal(line);
							}else if(line.startsWith("02")) {
								parssingEnd(line);
							}
						}
						br.close();
						
						logger.info("KCP 차액정산 데이터 개수 : [" + list.size() + "]");
						
						if(dao.updateTrxDiff(list) > 0) {
							logger.info("updateTrxCap [{}]",dao.updateTrxCap(nowDate));
						}
					}
				}
			}
			if(!fileCheck) {
				logger.info("차액정산 결과파일이 존재하지 않습니다!!");
			}
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.info("============= "+nowDate+"일자 차액정산 종료 ===============");
	}
	
	private void parssingStart(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, resBuf.length, resBuf, 0, resBuf.length);
		
		String recordType	= CommonUtil.toString(resBuf,0,2).trim();
		String date	= CommonUtil.toString(resBuf,2,8).trim();
		String bizRegNo	= CommonUtil.toString(resBuf,10,10).trim();
		String gubun	= CommonUtil.toString(resBuf,20,10).trim();
		String filler	= CommonUtil.toString(resBuf,30,370).trim();
	}
	
	private void parssingHeader(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, resBuf.length, resBuf, 0, resBuf.length);
		
		String recordType	= CommonUtil.toString(resBuf,0,2).trim();
		String siteCd	= CommonUtil.toString(resBuf,2,10).trim();
		String filler	= CommonUtil.toString(resBuf,12,388).trim();
	}
	
	private void parssingBody(String data) {
		String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType	= "R";
		String trxType	= CommonUtil.toString(resBuf,2,1).trim();
		String trxDay	= CommonUtil.toString(resBuf,3,8).trim(); 
		String compNo	= CommonUtil.toString(resBuf,11,10).trim();
		String mchtCompNo	= CommonUtil.toString(resBuf,21,10).trim();
		String vanTrxId	= CommonUtil.toString(resBuf,31,30).trim();
		String orgVanTrxId	= CommonUtil.toString(resBuf,61,30).trim();
		String trxId	= CommonUtil.toString(resBuf,91,70).trim();
		String mchtSalesAmt	= CommonUtil.toString(resBuf,161,15).trim();
		String amount	= CommonUtil.toString(resBuf,176,15).trim();
		String approvDay	= CommonUtil.toString(resBuf,191,8).trim();
		String mchtFiller	= CommonUtil.toString(resBuf,199,40).trim();
		
		String mchtCode = CommonUtil.toString(resBuf,239,1).trim();
		String mchtType = "";
		switch(mchtCode) {
			case "0":mchtType = "영세";break;
			case "1":mchtType = "중소1";break;
			case "2":mchtType = "중소2";break;
			case "3":mchtType = "중소3";break;
			case "4":mchtType = "일반";break;
		}

		String transType	= CommonUtil.toString(resBuf,240,1).trim();
		String cardType	= CommonUtil.toString(resBuf,241,1).trim();
		String diffStlFee	= CommonUtil.toString(resBuf,242,15).trim();
		String diffStlVat	= CommonUtil.toString(resBuf,257,15).trim();
		String diffStlAmt	= CommonUtil.toString(resBuf,272,15).trim();
		String diffStlDay	= CommonUtil.toString(resBuf,287,8).trim();
		String resultCd	= CommonUtil.toString(resBuf,295,2).trim();
		String filler	= CommonUtil.toString(resBuf,297,103).trim();
		
		SharedMap<String, Object> map = new SharedMap<String, Object>();
		map.put("recordType",recordType);
		map.put("trxId", trxId);
		map.put("resultCd", resultCd);
		map.put("mchtType", mchtType);
		map.put("mchtCode", mchtCode);
		map.put("cardType", cardType);
		map.put("diffStlAmt", diffStlAmt);
		map.put("diffStlDay", diffStlDay);
		map.put("downDay", nowDate);
		
		list.add(map);
	}
	
	private void parssingTotal(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType = CommonUtil.toString(resBuf,0,2).trim();
		String totCnt = CommonUtil.toString(resBuf,2,7).trim();
		String totAmt = CommonUtil.toString(resBuf,9,18).trim();
		String totDiffAmt = CommonUtil.toString(resBuf,27,15).trim();
		String filler = CommonUtil.toString(resBuf,42,388).trim();
	}
	
	private void parssingEnd(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType = CommonUtil.toString(resBuf,0,2).trim();
		String totLine = CommonUtil.toString(resBuf,2,7).trim();
		String filler = CommonUtil.toString(resBuf,9,391).trim();
	}
	
	public static void main(String[] args) {
		new KcpDiffTrxDownLoad(args[0]);
	}

	private boolean downloadFtp(String filePath) {
		boolean ftpUpload = true;
		
		try {
			logger.info("KCP Diff Trx FTP Download Start!!");
			
			Runtime runtime = Runtime.getRuntime();
	        Process process;
			process = runtime.exec(filePath);
		
	        InputStream is = process.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	
	        String line;
	
	        while((line = br.readLine()) != null) {
	        	logger.info("KCP Diff Trx FTP Download : " + line);
	        }
	        
	        logger.info("KCP Diff Trx FTP Download END!!");
		} catch (IOException e) {
			ftpUpload = false;
			logger.info("KCP Diff Trx FTP Download FAIL!!");
			logger.error(e.getMessage(), e);
		}
		return ftpUpload;
	}
}
