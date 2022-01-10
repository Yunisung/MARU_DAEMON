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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KsnetDiffDownloadDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class KcpDiffTrxReCheckDownLoad {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KcpDiffMchtDownLoad.class );

	private static String SETTLE_PATH="/home/data/diff/kcp/check/";
//	private static String MCHT_PATH="C:\\dev\\test\\mcht\\";
//	private static String SETTLE_PATH="C:\\dev\\test\\settle\\";
	private static String uploadPath = "../bin/check_download.sh";
	
	private String date = "";
	private String seq = "";
	private String siteCd = "";
	
	private List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
	
	public KcpDiffTrxReCheckDownLoad(String nowDate) {
		downLoadDiffTrx(nowDate);
	}
	
	public void downLoadDiffTrx(String nowDate) {
		try {
			configSetting();
			
			logger.info("============= "+date+"일자 KCP 차액정산 검증 재요청시작  ===============");
			
			boolean fileCheck = false;
			String path = SETTLE_PATH + date;
			
			KsnetDiffDownloadDAO dao = new KsnetDiffDownloadDAO();
			
			File folder = new File(path);
			if(!folder.exists()) {
				folder.mkdirs();
			}
			
			downloadFtp(uploadPath);

			File file = new File(path);
			File fileList[] = file.listFiles();
			
			for(int i = 0; i < fileList.length; i++) {
				logger.info("KCP 차액정산 재검증 Download File List : [" + fileList[i].getName() + "]");
				
				if(fileList[i].isFile()) {
					if(fileList[i].getName().equals("KCP_BCFS_FILE_DF9" + seq + "_OUT_"+ siteCd + "_" + date)) {
						fileCheck = true;
						File cvs = new File(path + fileList[i].getName());
						FileReader fr = new FileReader(cvs);
						BufferedReader br = new BufferedReader(fr);
						String line = "";
						
						while((line = br.readLine()) != null) {
							logger.info("KCP 차액정산 재검증 Data : [" + line + "]");
							
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
						
						logger.info("KCP 차액정산 재검증 데이터 개수 : [" + list.size() + "]");
						
						dao.updateTrxCheckDiff(list);
					}
				}
			}
			if(!fileCheck) {
				logger.info("KCP 차액정산 재검증 파일이 존재하지 않습니다!!");
			}
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		logger.info("============= "+date+"일자 KCP 차액정산 검증 종료 ===============");
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
		
		String recordType	= "F";
		String trxType	= CommonUtil.toString(resBuf,2,1).trim();
		String trxDay	= CommonUtil.toString(resBuf,3,8).trim(); 
		String compNo	= CommonUtil.toString(resBuf,11,10).trim();
		String mchtCompNo	= CommonUtil.toString(resBuf,21,10).trim();
		String vanTrxId	= CommonUtil.toString(resBuf,31,30).trim();
		String orgVanTrxId	= CommonUtil.toString(resBuf,61,30).trim();
		String trxId	= CommonUtil.toString(resBuf,91,70).trim();
		String mchtSalesAmt	= CommonUtil.toString(resBuf,161,15).trim();
		String amount	= CommonUtil.toString(resBuf,176,15).trim();
		String mchtFiller	= CommonUtil.toString(resBuf,199,40).trim();
		String errCode = CommonUtil.toString(resBuf,239,2).trim();
		String filler	= CommonUtil.toString(resBuf,241,159).trim();
		
		SharedMap<String, Object> map = new SharedMap<String, Object>();
		map.put("recordType",recordType);
		map.put("trxId", trxId);
		map.put("resultCd", errCode);
		
		list.add(map);
	}
	
	private void parssingTotal(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType = CommonUtil.toString(resBuf,0,2).trim();
		String totCnt = CommonUtil.toString(resBuf,2,7).trim();
		String totAmt = CommonUtil.toString(resBuf,9,18).trim();
		String resCd = CommonUtil.toString(resBuf,27,2).trim();
		String filler = CommonUtil.toString(resBuf,29,371).trim();
	}
	
	private void parssingEnd(String data) {
		byte[] resBuf = new byte[data.length()];
		System.arraycopy(data, 500, resBuf, 0, resBuf.length);
		
		String recordType = CommonUtil.toString(resBuf,0,2).trim();
		String totLine = CommonUtil.toString(resBuf,2,7).trim();
		String resCd = CommonUtil.toString(resBuf,9,2).trim();
		String filler = CommonUtil.toString(resBuf,11,389).trim();
	}
	
	public static void main(String[] args) {
		new KcpDiffTrxReCheckDownLoad(args[0]);
	}

	private boolean downloadFtp(String filePath) {
		boolean ftpUpload = true;
		
		try {
			logger.info("KCP 재검증 FTP Download Start!!");
			
			Runtime runtime = Runtime.getRuntime();
	        Process process;
			process = runtime.exec(filePath);
		
	        InputStream is = process.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	
	        String line;
	
	        while((line = br.readLine()) != null) {
	        	logger.info("KCP 재검증 FTP Download : " + line);
	        }
	        
	        logger.info("KCP 재검증 FTP Download END!!");
		} catch (IOException e) {
			ftpUpload = false;
			logger.info("KCP 재검증 FTP Download FAIL!!");
			logger.error(e.getMessage(), e);
		}
		return ftpUpload;
	}
	
	/**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            // 프로퍼티 파일 위치
            String propFile = "C:/workspace/MARU_DAEMON/conf/kcpdiffconfig.properties";

            // 프로퍼티 객체 생성
            Properties props = new Properties();

            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            // 항목 읽기
            String type = props.getProperty("type") ;
            
            seq = props.getProperty(type + "_seq");
            date = props.getProperty(type + "_date");
            siteCd = props.getProperty(type + "_siteCd");
           
            logger.info("config Data : seq [" + seq + "], date [" + date + "], siteCd [" + siteCd + "]");
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
