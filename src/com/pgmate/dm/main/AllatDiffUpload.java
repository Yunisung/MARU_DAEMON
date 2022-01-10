package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.AllatDiffUploadDAO;
import com.pgmate.dm.dao.KcpDiffUploadDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class AllatDiffUpload {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.AllatDiffUpload.class);

	// private static String MCHT_PATH="/home/data/diff/kcp/mcht/";
	// private static String SETTLE_PATH="/home/data/diff/kcp/settle/";
	private static String MCHT_PATH = "C:\\dev\\diff\\mcht\\";
	private static String SETTLE_PATH = "C:\\dev\\diff\\settle\\";
	private static String mchtUploadPath = "../bin/mcht_upload.sh";
	private static String settleUploadPath = "../bin/settle_upload.sh";

	private String[] cardCodeArr = { "CNB", "NLC", "BCC", "DIN", "LGC", "KEB", "AMX", "WIN" };
	private String siteCd = "T0000";
	private int totCnt = 0;
	private int dataCnt = 0;
	private long dataAmt = 0;

	public static void main(String[] args) {
		new AllatDiffUpload();
	}

	public AllatDiffUpload() {
		try {
			makeDiffMcht();
			makeDiffSettle();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * ALLAT 2차이하 PG 하위사업자 정보 등록
	 * 
	 * @throws IOException
	 */
	public void makeDiffMcht() throws IOException {
		AllatDiffUploadDAO dao = new AllatDiffUploadDAO();
		BufferedWriter bw = null;

		try {
			logger.info("============== ALLAT 하위사업자 등록 배치 시작 ==============");
			
			List<SharedMap<String, Object>> mchtList = dao.getMchtList();
			
			if (mchtList.size() > 0) {
				String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
				String path = MCHT_PATH + nowDate.substring(0, 6);
				String fileName = path + File.separator + "ALLAT_BCFS_FILE_SR00_INX_" + siteCd + "_" + nowDate + ".txt";

				File folder = new File(path);
				if (!folder.exists()) {
					folder.mkdirs();
				}

				logger.info("ALLAT 하위사업자 등록 파일명 [{}]", fileName);

				File file = new File(fileName);
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "euc-kr"));

				logger.info("ALLAT diffMcht Data Setting Start");
				headerDiffMchtDataSetting(bw, nowDate);
				bodyDiffMchtDataSetting(bw, mchtList, nowDate);
				tailDiffMchtDataSetting(bw);
				logger.info("ALLAT diffMcht Data Setting End");

				bw.flush();
				bw.close();

				dao.updateDiffMcht(mchtList);

				boolean uploadCheck = uploadDiffFtpFile(mchtUploadPath);
				
				if(uploadCheck) { 
					if(mchtList.size()>0) { 
						dao.updateDiffMcht(mchtList); 
					} 
				}
			} else {
				logger.info("ALLAT 하위사업자 등록할 정보가 존재하지 않습니다.");
			}
			
			logger.info("============== ALLAT 하위사업자 등록 배치 종료 ==============");
		} catch (Exception e) {
			logger.error("makeDiffMcht Error : " + e.getMessage(), e);
		} finally {
			if (bw != null) {
				bw.close();
			}
		}
	}

	/**
	 * ALLAT 하위사업자 등록 해더전문
	 * 
	 * @param bw
	 * @param date
	 * @throws IOException
	 */
	private void headerDiffMchtDataSetting(BufferedWriter bw, String date) throws IOException {
		StringBuffer headerData = new StringBuffer();

		headerData.append(CommonUtil.byteFiller("HD", 2)); // 레코드구분
		headerData.append(CommonUtil.zerofill(date, 8)); // 요청일자
		headerData.append(CommonUtil.byteFiller("", 490));

		bw.write(headerData.toString());
		bw.newLine();
	}

	/**
	 * ALLAT 하위사업자 등록 바디전문
	 * 
	 * @param bw
	 * @param mchtList
	 * @param date
	 * @throws IOException
	 */
	private void bodyDiffMchtDataSetting(BufferedWriter bw, List<SharedMap<String, Object>> mchtList, String date)
			throws IOException {
		StringBuffer bodyData = new StringBuffer();
		String gubun = "|";
		
		for (SharedMap<String, Object> map : mchtList) {
			for (int i = 0; i < cardCodeArr.length; i++) {
				bodyData = new StringBuffer();

				bodyData.append(CommonUtil.byteFiller("RD", 2) + gubun); // 레코드구분
				bodyData.append(CommonUtil.byteFiller(map.getString("regType"), 2) + gubun); // 등록구분
				bodyData.append(CommonUtil.byteFiller("AAT", 3) + gubun); // PG사 기관코드
				bodyData.append(CommonUtil.byteFiller(cardCodeArr[i], 3) + gubun); // 카드사코드
				bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo").replace("-", ""), 10) + gubun); // 사업자등록번호(하위몰)
				bodyData.append(getRPad(map.getString("bizType"), 20, " ") + gubun); // 업종명
				bodyData.append(getRPad(map.getString("mchtName"), 40, " ") + gubun); // 회사명
				bodyData.append(getRPad(map.getString("mchtUrl"), 80, " ") + gubun); // 웹사이트 URL(하위몰)
				bodyData.append(getRPad(map.getString("addr1") + map.getString("addr2"), 100, " ") + gubun); // 주소(하위몰)
				bodyData.append(getRPad(map.getString("mchtCeo"), 40, " ") + gubun); // 대표자명(하위몰)
				bodyData.append(getRPad(map.getString("mchtPhone"), 11, " ") + gubun); // 전화번호(하위몰)
				bodyData.append(getRPad(map.getString("mchtEmail"), 40, " ") + gubun); // 이메일(하위몰)
				bodyData.append(getRPad(date, 8, " ") + gubun); // 정보등록일
				bodyData.append(getRPad(map.getString("mchtId"), 20, " ")); // 상점아이디
			
				logger.info("ALLAT MCHT DATA FILE : [" + bodyData.toString() + "]");

				bw.write(bodyData.toString());
				bw.newLine();

				dataCnt++;
			}
		}
	}

	
	/**
	 * ALLAT 삼성카드 하위사업자 등록 바디전문
	 * 
	 * @param bw
	 * @param mchtList
	 * @param date
	 * @throws IOException
	 */
	private void bodyDiffMchtSamsungCardDataSetting(BufferedWriter bw, List<SharedMap<String, Object>> mchtList, String date)
			throws IOException {
		StringBuffer bodyData = new StringBuffer();
		String gubun = "|";
		
		for (SharedMap<String, Object> map : mchtList) {
			for (int i = 0; i < cardCodeArr.length; i++) {
				bodyData = new StringBuffer();

				bodyData.append(CommonUtil.zerofill(map.getString("compNo").replace("-", ""), 10)); // 오픈마켓 사업자번호
				bodyData.append(getRPad("", 20, " ")); // 가맹번호
				bodyData.append(getRPad(map.getString("mchtName"), 40, " ")); // 오픈마켓 회사명
				bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo").replace("-", ""), 10)); // 사업자등록번호(하위몰)
				bodyData.append(getRPad(map.getString("bizType"), 20, " ")); // 업종명
				bodyData.append(getRPad(map.getString("mchtName"), 40, " ")); // 하위몰 상호명
				bodyData.append(CommonUtil.zerofill(map.getString("zip"), 6)); // 우편번호(하위몰)
				bodyData.append(getRPad(map.getString("addr1") + map.getString("addr2"), 100, " ") + gubun); // 주소(하위몰)
				bodyData.append(getRPad(map.getString("mchtCeo"), 40, " ") + gubun); // 대표자명(하위몰)
				bodyData.append(getRPad(map.getString("mchtPhone"), 11, " ") + gubun); // 전화번호(하위몰)
				bodyData.append(getRPad(map.getString("mchtUrl"), 80, " ") + gubun); // 웹사이트 URL(하위몰)
				bodyData.append(getRPad(map.getString("mchtEmail"), 40, " ") + gubun); // 이메일(하위몰)
			
				logger.info("ALLAT MCHT SAMSUNG CARD DATA FILE : [" + bodyData.toString() + "]");

				bw.write(bodyData.toString());
				bw.newLine();

				dataCnt++;
			}
		}
	}
	
	/**
	 * 하위사업자 등록 테일전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void tailDiffMchtDataSetting(BufferedWriter bw) throws IOException {
		StringBuffer tailData = new StringBuffer();

		tailData.append(CommonUtil.byteFiller("TR", 2)); // 레코드구분
		tailData.append(CommonUtil.zerofill(dataCnt, 10)); // 총 전송 건수
		tailData.append(CommonUtil.zerofill(dataCnt, 10)); // 신규 전송 건수
		tailData.append(CommonUtil.zerofill("0000000000", 10));
		tailData.append(CommonUtil.zerofill("0000000000", 10));
		tailData.append(CommonUtil.byteFiller("", 458));

		bw.write(tailData.toString());
	}

	/**
	 * 영중소 가맹점 우대수수료 차액정산
	 * 
	 * @throws IOException
	 */
	public void makeDiffSettle() throws IOException {
		
		String nowDate = CommonUtil.getCurrentDate("yyyyMMdd");
		
		GregorianCalendar today = new GregorianCalendar();
		today.add(Calendar.DATE, -1);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String yesterDay = format.format(today.getTime());

		AllatDiffUploadDAO dao = new AllatDiffUploadDAO();
		BufferedWriter bw = null;

		try {
			logger.info("============== ALLAT 차액정산 배치 시작 ==============");
			
			List<SharedMap<String, Object>> payList = dao.getPayList();
			List<SharedMap<String, Object>> rfdList = dao.getRfdList();
			
			if (payList.size() > 0 || rfdList.size() > 0) {
				String path = SETTLE_PATH + nowDate.substring(0, 6);
				String fileName = path + File.separator + "ALLAT_BCFS_FILE_DF01_INX_" + siteCd + "_" + yesterDay + ".txt";

				File folder = new File(path);
				if (!folder.exists()) {
					folder.mkdirs();
				}
				logger.info("ALLAT 차액정산 파일명 [{}]", fileName);

				File file = new File(fileName);
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "euc-kr"));

				logger.info("ALLAT diffMcht Data Setting Start");
				//startDiffSettleDataSetting(bw, nowDate);
				headerDiffSettleDataSetting(bw);
				dataDiffSettleDataSetting(bw, payList, rfdList);
				tailDiffSettleDataSetting(bw);
				//endDiffSettleDataSetting(bw);
				logger.info("ALLAT diffMcht Data Setting End");

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
						logger.info("ALLAT 차액정산 DB처리 성공!!"); 
					} 
				}else {
					logger.info("ALLAT 차액정산 FTP 업로드 실패!!"); 
				}
			} else {
				logger.info("ALLAT 차액정산 정보가 존재하지 않습니다.");
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
	 * 영중소 가맹점 차액정산 Name전문
	 * 
	 * @param bw
	 * @param date
	 * @throws IOException
	 */
	private void nameDiffSettleDataSetting(BufferedWriter bw, String date) throws IOException {
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
	 * 영중소 가맹점 차액정산 데이터전문
	 * 
	 * @param bw
	 * @throws IOException
	 */
	private void dataDiffSettleDataSetting(BufferedWriter bw, List<SharedMap<String, Object>> payList,
			List<SharedMap<String, Object>> rfdList) throws IOException {
		StringBuffer bodyData = new StringBuffer();
		int seq = 1;
		
		for (SharedMap<String, Object> map : payList) {
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("D", 1)); // line속성
			bodyData.append(CommonUtil.byteFiller(seq, 10)); // 일련번호
			bodyData.append(CommonUtil.byteFiller(map.getString("trxType"), 1)); // 승인취소 구분
			bodyData.append(CommonUtil.byteFiller(map.getString("rfdTurn"), 2)); // 부부취소 횟수
			
			
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

			logger.info("ALLAT APPR SETTLE DATA FILE : [" + bodyData.toString() + "]");

			bw.write(bodyData.toString());
			bw.newLine();

			totCnt++;
			dataCnt++;
			dataAmt = dataAmt + map.getLong("amount");
		}

		for (SharedMap<String, Object> map : rfdList) {
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("D", 1)); // line속성
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

			logger.info("ALLAT REFUND SETTLE DATA FILE : [" + bodyData.toString() + "]");

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

	private String getSeq(int idx) {
		String nowDate = CommonUtil.getCurrentDate("yyMMdd");

		String seq = nowDate + "_" + CommonUtil.zerofill(idx, 5);

		return seq;
	}

	private boolean uploadDiffFtpFile(String filePath) {
		boolean ftpUpload = true;

		try {
			logger.info("ALLAT 파일 업로드 시작!!");

			Runtime runtime = Runtime.getRuntime();
			Process process;
			process = runtime.exec(filePath);

			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			String line;

			while ((line = br.readLine()) != null) {
				logger.info("ALLAT FTP : " + line);
			}

			logger.info("ALLAT 파일 업로드 종료!!");
		} catch (IOException e) {
			ftpUpload = false;
			logger.info("ALLAT 파일업로드 실패!!");
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
}
