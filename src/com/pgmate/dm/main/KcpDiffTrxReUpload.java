package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.KcpDiffUploadDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class KcpDiffTrxReUpload {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.KcpDiffTrxReUpload.class);

	// private static String MCHT_PATH="/home/data/diff/kcp/mcht/";
	// private static String SETTLE_PATH="/home/data/diff/kcp/settle/";
	private static String SETTLE_PATH = "C:\\dev\\diff\\settle\\";
	private static String settleUploadPath = "../bin/resettle_upload.sh";

	private int totCnt = 0;
	private int dataCnt = 0;
	private long dataAmt = 0;
	private String date = "";
	private String seq = "";
	private String siteCd = "";
	
	public static void main(String[] args) {
		new KcpDiffTrxReUpload();
	}

	public KcpDiffTrxReUpload() {
		try {
			makeDiffSettle();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
	
	/**
	 * 영중소 가맹점 우대수수료 차액정산
	 * 
	 * @throws IOException
	 */
	public void makeDiffSettle() throws IOException {
		BufferedWriter bw = null;
		
		try {
			configSetting();
			
			String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
			KcpDiffUploadDAO dao = new KcpDiffUploadDAO();

			logger.info("============== KCP 차액정산 재요청 배치 시작 ==============");
			List<SharedMap<String, Object>> payList = dao.getRePayList(date);
			List<SharedMap<String, Object>> rfdList = dao.getReRfdList(date);
			
			if (payList.size() > 0 || rfdList.size() > 0) {
				String path = SETTLE_PATH + nowDate.substring(0, 6);
				String fileName = path + File.separator + "KCP_BCFS_FILE_DF" + seq + "_INX_" + siteCd + "_" + date + ".txt";

				File folder = new File(path);
				if (!folder.exists()) {
					folder.mkdirs();
				}
				logger.info("KCP 차액정산 재요청 파일명 [{}]", fileName);

				File file = new File(fileName);
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "euc-kr"));

				logger.info("KCP diffMcht Data Setting Start");
				startDiffSettleDataSetting(bw, nowDate);
				headerDiffSettleDataSetting(bw);
				bodyDiffSettleDataSetting(bw, payList, rfdList);
				tailDiffSettleDataSetting(bw);
				endDiffSettleDataSetting(bw);
				logger.info("KCP diffMcht Data Setting End");

				bw.close();

				boolean uploadCheck = uploadDiffFtpFile(settleUploadPath);
				
				if(uploadCheck) { 
					if(payList.size()>0 || rfdList.size()>0) {
						if(payList.size()>0) { 
							dao.insertTrxDiffUpload(payList, nowDate); 
						}
						if(rfdList.size()>0) { 
							dao.insertTrxDiffUpload(rfdList, nowDate); 
						}
						logger.info("KCP 차액정산 재요청 DB처리 성공!!"); 
					} 
				}else {
					logger.info("KCP 차액정산 재요청 FTP 업로드 실패!!"); 
				}
			} else {
				logger.info("KCP 차액정산 재요청 정보가 존재하지 않습니다.");
			}
		} catch (Exception e) {
			logger.error("makeDiffSettle Error : " + e.getMessage(), e);
		} finally {
			if (bw != null) {
				bw.close();
			}
		}
	}

	/**
	 * 영중소 가맹점 차액정산 시작전문
	 * 
	 * @param bw
	 * @param date
	 * @throws IOException
	 */
	private void startDiffSettleDataSetting(BufferedWriter bw, String date) throws IOException {
		StringBuffer startData = new StringBuffer();
		dataCnt = 0;
		dataAmt = 0;
		totCnt++;

		startData.append(CommonUtil.byteFiller("01", 2)); // 레코득구분
		startData.append(CommonUtil.byteFiller(date, 8)); // 요청일자
		startData.append(CommonUtil.byteFiller("4198800046", 10)); // 가맹점 사업자번호
		startData.append(CommonUtil.byteFiller("NHNKCP", 10)); // PG사구분
		startData.append(CommonUtil.byteFiller("", 370));

		bw.write(startData.toString());
		bw.newLine();
	}

	/**
	 * 영중소 가맹점 차액정산 헤더전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void headerDiffSettleDataSetting(BufferedWriter bw) throws IOException {
		StringBuffer headerData = new StringBuffer();
		totCnt++;

		headerData.append(CommonUtil.byteFiller("10", 2)); // 레봌드구분
		headerData.append(CommonUtil.byteFiller(siteCd, 10)); // 서비스구분코드
		headerData.append(CommonUtil.byteFiller("", 388));

		bw.write(headerData.toString());
		bw.newLine();
	}

	/**
	 * 영중소 가맹점 차액정산 바디전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void bodyDiffSettleDataSetting(BufferedWriter bw, List<SharedMap<String, Object>> payList,
			List<SharedMap<String, Object>> rfdList) throws IOException {
		StringBuffer bodyData = new StringBuffer();

		for (SharedMap<String, Object> map : payList) {
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("11", 2)); // 레코드구분
			bodyData.append(CommonUtil.byteFiller(map.getString("trxType"), 1)); // 승인취소 구분
			bodyData.append(CommonUtil.byteFiller(map.getString("trxDay"), 8)); // 거래일자`
			bodyData.append(CommonUtil.byteFiller(map.getString("compNo"), 10)); // 중간 사업자등록번호(하위몰)
			bodyData.append(CommonUtil.byteFiller(map.getString("mchtCompNo"), 10)); // 최종 사업자등록번호(하위몰)
			bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 30)); // PG거래번호(TID)
			bodyData.append(CommonUtil.byteFiller(map.getString(""), 30)); // PG원거래거래번호(TID)
			bodyData.append(getRPad(map.getString("trxId"), 70, " ")); // 가맹점 주문번호
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15)); // 하위사업자 매출액
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15)); // 원거래 매입금액
			bodyData.append(CommonUtil.byteFiller(map.getString("trxDay"), 8)); // 승인일자
			bodyData.append(CommonUtil.byteFiller(map.getString("filler"), 40));
			bodyData.append(CommonUtil.byteFiller("", 161));

			logger.info("KCP APPR SETTLE DATA FILE : [" + bodyData.toString() + "]");

			bw.write(bodyData.toString());
			bw.newLine();

			totCnt++;
			dataCnt++;
			dataAmt = dataAmt + map.getLong("amount");
		}

		for (SharedMap<String, Object> map : rfdList) {
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("11", 2)); // 레코드구분
			bodyData.append(CommonUtil.byteFiller(map.getString("trxType"), 1)); // 승인취소 구분
			bodyData.append(CommonUtil.byteFiller(map.getString("trxDay"), 8)); // 거래일자`
			bodyData.append(CommonUtil.byteFiller(map.getString("compNo"), 10)); // 중간 사업자등록번호(하위몰)
			bodyData.append(CommonUtil.byteFiller(map.getString("mchtCompNo"), 10)); // 최종 사업자등록번호(하위몰)
			bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 30)); // PG거래번호(TID)
			bodyData.append(CommonUtil.byteFiller(map.getString("rootTrxId"), 30)); // PG원거래거래번호(TID)
			bodyData.append(getRPad(map.getString("trxId"), 70, " ")); // 가맹점 주문번호
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15)); // 하위사업자 매출액
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15)); // 원거래 매입금액
			bodyData.append(CommonUtil.byteFiller(map.getString("trxDay"), 8)); // 승인일자
			bodyData.append(CommonUtil.byteFiller(map.getString("filler"), 40));
			bodyData.append(CommonUtil.byteFiller("", 161));

			logger.info("KCP REFUND SETTLE DATA FILE : [" + bodyData.toString() + "]");

			bw.write(bodyData.toString());
			bw.newLine();

			totCnt++;
			dataCnt++;
			dataAmt = dataAmt - map.getLong("amount");
		}
	}

	/**
	 * 영중소 가맹점 차액정산 테일전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void tailDiffSettleDataSetting(BufferedWriter bw) throws IOException {
		StringBuffer tailData = new StringBuffer();
		totCnt++;

		tailData.append(CommonUtil.byteFiller("12", 2)); // 레코드구분
		tailData.append(CommonUtil.zerofill(dataCnt, 7)); // 총 건수 합계
		tailData.append(CommonUtil.zerofill(dataAmt, 10)); // 총 매출액 합계
		tailData.append(CommonUtil.byteFiller("", 373));

		bw.write(tailData.toString());
	}

	/**
	 * 영중소 가맹점 차액정산 끝전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void endDiffSettleDataSetting(BufferedWriter bw) throws IOException {
		StringBuffer tailData = new StringBuffer();
		totCnt++;

		tailData.append(CommonUtil.byteFiller("08", 2)); // 레코드 구분
		tailData.append(CommonUtil.zerofill(totCnt, 7)); // 전체 줄 수
		tailData.append(CommonUtil.byteFiller("", 391));

		bw.write(tailData.toString());
	}

	private String getSeq(int idx) {
		String nowDate = CommonUtil.getCurrentDate("yyMMdd");

		String seq = nowDate + "_" + CommonUtil.zerofill(idx, 5);

		return seq;
	}

	private boolean uploadDiffFtpFile(String filePath) {
		boolean ftpUpload = true;

		try {
			logger.info("KCP FTP Start!!");

			Runtime runtime = Runtime.getRuntime();
			Process process;
			process = runtime.exec(filePath);

			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			String line;

			while ((line = br.readLine()) != null) {
				logger.info("KCP FTP : " + line);
			}

			logger.info("KCP FTP END!!");
		} catch (IOException e) {
			ftpUpload = false;
			logger.info("KCP FTP FAIL!!");
			logger.error(e.getMessage(), e);
		}
		return ftpUpload;
	}

	/**
	 * 오른쪽으로 자리수만큼 문자 채우기
	 *
	 * @param str         원본 문자열
	 * @param size        총 문자열 사이즈(리턴받을 결과의 문자열 크기)
	 * @param strFillText 원본 문자열 외에 남는 사이즈만큼을 채울 문자
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String getRPad(String str, int size, String strFillText) throws UnsupportedEncodingException {
		int len = (str.getBytes("euc-kr")).length;

		for (int i = len; i < size; i++) {
			str += strFillText;
		}
		return str;
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
