package com.pgmate.dm.main;

import com.jcraft.jsch.JSch;
import com.pgmate.dm.dao.GalaxiaDiffUploadDAO;
import com.pgmate.dm.dao.KcpDiffUploadDAO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalaxiaDiffUpload {
	private Logger logger = LoggerFactory.getLogger(getClass());

	//JM VMWARE SFTP SERVER
//	private static String HOST = "192.168.95.139";
	//GALAXIA SFTP SERVER
	private static String HOST = "119.207.70.214";
	private static int PORT = 22;

	//JM VMWARE SFTP SERVER
//	final String userId = "mysftpuser";
//	final String userPw = "1234";
	//GALAXIA SFTP SERVER
	private static String userId = "A2240732";
	private static String userPw = "1!qnrnrdnlsjtm0732";

	//BK 서버 업로드 파일 저장 경로
	private static String MCHT_PATH="/home/bkwinners/diff/mcht_galaxia/";
	private static String SETTLE_PATH="/home/bkwinners/diff/settle_galaxia/";
//	private static String MCHT_PATH="D:\\galaxia\\diffMcht\\";
//	private static String SETTLE_PATH="D:\\galaxia\\diffSettle\\";

//	private static String GALAXIA_UPLOAD_PATH="/test";		//테스트 폴더
	private static String GALAXIA_UPLOAD_PATH="/request";	//운영 폴더


	private SmsGw smsGw = null;
	private String nowDate = "";
	private int dataCnt = 0;
	private long dataAmt = 0;

	public GalaxiaDiffUpload() {
		try {
			makeDiffMcht();		//하위사업자 등록
			makeDiffSettle();	//차액정산 등록
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 하위사업자 등록
	 */
	public void makeDiffMcht() {
		logger.info("========== GALAXIA 하위사업자 등록 START ==========");

		nowDate = CommonUtil.getCurrentDate("yyyyMMdd");

		String uploadPath = MCHT_PATH + nowDate.substring(0, 6);
		String fileName = userId + "_REQUEST_INFO." + nowDate;

		GalaxiaDiffUploadDAO dao = new GalaxiaDiffUploadDAO();

		File folder = new File(uploadPath);
		if(!folder.exists()) {
			folder.mkdir();
		}

		final SFTPUtil sftpUtil = new SFTPUtil();

		//SFTP 서버 접속
		sftpUtil.init(HOST, userId, userPw, PORT);

		//파일명 생성
		uploadPath += File.separator + fileName;

		logger.info("DIFF MCHT UPLOAD FILE NAME ===> {}", uploadPath);

		//파일 객체 생성
		File uploadFile = new File(uploadPath);

		try{
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile), "euc-kr"));

			List<SharedMap<String, Object>> mchtList = dao.getMchtList();

			logger.info("GALAXIA DIFFMCHT DATA SETTING START");
			headerMchtSetting(bw);
			dataMchtSetting(bw, mchtList);
			totalMchtSetting(bw);
			bw.flush();
			bw.close();
			logger.info("GALAXIA DIFFMCHT DATA SETTING END");

			//GALAXIA 파일 업로드
			if(sftpUtil.upload(GALAXIA_UPLOAD_PATH, uploadFile)){
				logger.info("===== GALAXIA DIFFMCHT UPLOAD SUCCESSS =====");
				if(mchtList.size() > 0) {
					dao.updateDiffMcht(mchtList);
				}
			} else {
				logger.info("===== GALAXIA DIFFMCHT UPLOAD FAIL =====");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

//		sftpUtil.download(GALAXIA_UPLOAD_PATH, "가맹점AID_RECEIVE_INFO.20230104", DOWNLOAD_PATH+"\\DOWNLOADFILE");
//		File downloadFile = new File(DOWNLOAD_PATH+"\\DOWNLOADFILE");

		sftpUtil.disconnection();

		logger.info("========== GALAXIA 하위사업자 등록 END ==========");
	}

	/**
	 * 하위사업자 등록 해더전문
	 *
	 * @param bw
	 * @throws IOException
	 */
	public void headerMchtSetting(BufferedWriter bw) throws IOException {
		StringBuffer headerData = new StringBuffer();

		headerData.append(CommonUtil.byteFiller("HD", 2));	//레코드 구분
		headerData.append(CommonUtil.zerofill(nowDate, 8));	//파일생성일자
		headerData.append(CommonUtil.byteFiller("", 490));	//공백

		bw.write(headerData.toString());
		bw.newLine();
	}

	/**
	 * 하위사업자 등록 데이터전문
	 *
	 * @param bw
	 * @param mchtList
	 * @throws IOException
	 */
	public void dataMchtSetting(BufferedWriter bw, List<SharedMap<String, Object>> mchtList) throws IOException {
		StringBuffer bodyData = new StringBuffer();

		for(SharedMap<String, Object> map : mchtList) {
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("RD", 2));	//레코드구분
			bodyData.append(CommonUtil.zerofill(map.getString("regType"), 2));	//등록구분
			bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//오픈마켓 사업자번호
			bodyData.append(CommonUtil.zerofill("099", 3));	//카드사코드
			bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo").replace("-", ""), 10));	//사업자등록번호(하위몰)
			bodyData.append(getRPad(map.getString("bizType"), 20, " "));	//업종명(하위몰)
			bodyData.append(getRPad(map.getString("mchtName"), 40, " "));	//회사명(하위몰)
			bodyData.append(CommonUtil.zerofill(map.getString("zip"), 6));	//우편번호
			bodyData.append(getRPad(map.getString("addr1") + map.getString("addr2"), 100, " "));	//주소(하위몰)
			bodyData.append(getRPad(map.getString("mchtCeo"), 40, " "));	//대표자명(하위몰)
			cutAndSetTel(bodyData, map.getString("mchtPhone"));	//전화번호(하위몰)
			bodyData.append(getRPad(map.getString("mchtEmail"), 40, " "));	//이메일(하위몰)
			bodyData.append(getRPad(map.getString("mchtUrl"), 80, " "));	//웹사이트URL(하위몰)
			bodyData.append(getRPad(nowDate, 8, " "));	//정보등록일
			bodyData.append(CommonUtil.byteFiller("", 128));	//가맹점검증값(12) + 공백(116)

			bw.write(bodyData.toString());
			bw.newLine();

			dataCnt++;
		}
		logger.info("GALAXIA DIFFMCHT {} DATA", dataCnt);
	}

	/**
	 * 하위사업자 등록 토탈전문
	 *
	 * @param bw
	 * @throws IOException
	 */
	public void totalMchtSetting(BufferedWriter bw) throws IOException {
		StringBuffer totalData = new StringBuffer();

		totalData.append(CommonUtil.byteFiller("TR", 2));	//레코드구분
		totalData.append(CommonUtil.zerofill(dataCnt, 10));	//총 전송 건수
		totalData.append(CommonUtil.zerofill(dataCnt, 10));	//총 신규 건수
		totalData.append(CommonUtil.zerofill("0000000000", 10));	//총 변경 건수
		totalData.append(CommonUtil.zerofill("0000000000", 10));	//총 해지 건수
		totalData.append(CommonUtil.byteFiller("", 458));	//공백

		bw.write(totalData.toString());

		//초기화
		dataCnt = 0;
	}

	/**
	 * 영중소 가맹점 우대수수료 차액정산
	 *
	 * @throws IOException
	 */
	public void makeDiffSettle() throws IOException {
		logger.info("========== GALAXIA 차액정산 등록 START ==========");

		nowDate = CommonUtil.getCurrentDate("yyyyMMdd");

		String uploadPath = SETTLE_PATH + nowDate.substring(0, 6);
		String fileName = userId + "_REQUEST." + nowDate;

		File folder = new File(uploadPath);
		if(!folder.exists()) {
			folder.mkdir();
		}

		final SFTPUtil sftpUtil = new SFTPUtil();

		//SFTP 서버 접속
		sftpUtil.init(HOST, userId, userPw, PORT);

		uploadPath += File.separator + fileName;
		logger.info("DIFF SETTLE UPLOAD FILE NAME ===> {}", uploadPath);

		GalaxiaDiffUploadDAO dao = new GalaxiaDiffUploadDAO();
		BufferedWriter bw = null;

		//파일 객체 생성
		File uploadFile = new File(uploadPath);

		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile), "euc-kr"));

			List<SharedMap<String, Object>> payList = dao.getPayList();
			List<SharedMap<String, Object>> rfdList = dao.getRfdList();

			logger.info("GALAXIA DIFFSETTLE DATA SETTING START");
			headerDiffSetting(bw);
			if(payList.size() > 0 || rfdList.size() >0) {
				dataDiffSetting(bw, payList, rfdList);
			}
			totalDiffSetting(bw);
			bw.close();
			logger.info("GALAXIA DIFFSETTLE DATA SETTING END");

			//GALAXIA 파일 업로드
			if(sftpUtil.upload(GALAXIA_UPLOAD_PATH, uploadFile)){
				logger.info("===== GALAXIA DIFF SETTLE UPLOAD SUCCESSS =====");
				//----------------> 확인 필요
				if(payList.size() > 0) {
					dao.insertTrxDiffUpload(payList, nowDate);
				}
				if(rfdList.size() > 0) {
					dao.insertTrxDiffUpload(rfdList, nowDate);
				}
			} else {
				logger.info("===== GALAXIA DIFFSETTLE UPLOAD FAIL =====");
			}
		} catch (Exception e) {
			logger.error("MAKE DIFF SETTLE ERROR ===> " + e.getMessage(), e);
		} finally {
			if(bw != null) {
				bw.close();
			}
		}
	}

	/**
	 * 차액정산 등록 헤더전문
	 *
	 * @param bw
	 * @throws IOException
	 */
	private void headerDiffSetting(BufferedWriter bw) throws IOException {
		StringBuffer headerData = new StringBuffer();

		headerData.append(CommonUtil.byteFiller("HD", 2));	//레코드구분
		headerData.append(CommonUtil.zerofill(nowDate, 8));	//파일생성일자
		headerData.append(CommonUtil.byteFiller(userId, 20));	//가맹점 AID
		headerData.append(CommonUtil.byteFiller("", 147));	//공백

		bw.write(headerData.toString());
		bw.newLine();
	}

	/**
	 * 차액정산 등록 데이터전문
	 *
	 * @param bw
	 * @param payList
	 * @param rfdList
	 * @throws IOException
	 */
	private void dataDiffSetting(BufferedWriter bw, List<SharedMap<String, Object>> payList, List<SharedMap<String, Object>> rfdList) throws IOException {
		StringBuffer bodyData = new StringBuffer();

		for (SharedMap<String, Object> map : payList){
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("DT", 2));	//레코드구분
			bodyData.append(CommonUtil.zerofill("0", 1));		//매입취소구분
			bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//거래일자
			bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//중간하위사업자번호
			bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo"), 10));	//최종하위사업자번호
			bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 20));	//PG거래번호
			bodyData.append(CommonUtil.zerofill("01", 2));	//거래 순번
			bodyData.append(CommonUtil.byteFiller(map.getString("trxId"), 64));	//가맹점 주문번호
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//하위사업자 매출액
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//원거래 매입금액
			bodyData.append(CommonUtil.byteFiller("", 30));	//가맹점 검증값

			logger.info("GALAXIA PAY SETTLE DATA FILE : [" + bodyData.toString() + "]");

			bw.write(bodyData.toString());
			bw.newLine();

			dataCnt++;
			dataAmt = dataAmt + map.getLong("amount");
		}

		int rfdCnt = 2;
		for (SharedMap<String, Object> map : rfdList){
			bodyData = new StringBuffer();

			bodyData.append(CommonUtil.byteFiller("DT", 2));	//레코드구분
			bodyData.append(CommonUtil.zerofill("1", 1));		//매입취소구분
			bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//거래일자
			bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//중간하위사업자번호
			bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo"), 10));	//최종하위사업자번호
			bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 20));	//PG거래번호
			bodyData.append(CommonUtil.zerofill("02", 2));	//거래 순번
			bodyData.append(CommonUtil.byteFiller(map.getString("trxId"), 64));	//가맹점 주문번호
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//하위사업자 매출액
			bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//원거래 매입금액
			bodyData.append(CommonUtil.byteFiller("", 30));	//가맹점 검증값

			logger.info("GALAXIA REFUND SETTLE DATA FILE : [" + bodyData.toString() + "]");

			bw.write(bodyData.toString());
			bw.newLine();

			dataCnt++;
			dataAmt = dataAmt + map.getLong("amount");

			rfdCnt ++;
		}
	}

	/**
	 * 차액정산 등록 토탈전문
	 *
	 * @param bw
	 * @throws IOException
	 */
	private void totalDiffSetting(BufferedWriter bw) throws IOException {
		StringBuffer totalData = new StringBuffer();

		totalData.append(CommonUtil.byteFiller("TR", 2));	//레코드구분
		totalData.append(CommonUtil.zerofill(dataCnt, 7));		//총 건수 합계
		totalData.append(CommonUtil.zerofill(dataAmt, 18));	//총 매출액 합계
		totalData.append(CommonUtil.byteFiller("", 150));

		bw.write(totalData.toString());
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
	 * 전화번호 설정
	 *
	 * @param bodyData
	 * @param tel	전화번호
	 * @throws IOException
	 */
	public static void cutAndSetTel(StringBuffer bodyData, String tel) throws IOException {
		//(XX/XXX)-(XXX/XXXX)-XXXX 정규식
		Pattern tellPattern = Pattern.compile("^(01\\d{1}|02|0505|0502|0506|0\\d{1,2})-?(\\d{3,4})-?(\\d{4})");
		//XXXX-XXXX 정규식
		Pattern tellPattern2 = Pattern.compile("^(\\d{4})-?(\\d{4})");
		//'-'를 빈값으로 치환한 전화번호
		String replaceTel = tel.replace("-", "");

		//(XX/XXX)-(XXX/XXXX)-XXXX 형식의 전화번호 세팅
		Matcher matcher = tellPattern.matcher(replaceTel);
		if(matcher.matches()) {
			bodyData.append(getRPad(matcher.group(1), 3, " "));
			bodyData.append(getRPad(matcher.group(2), 4, " "));
			bodyData.append(CommonUtil.zerofill(matcher.group(3), 4));
		} else {
			//XXXX-XXXX 형식의 전화번호 세팅
			matcher = tellPattern2.matcher(replaceTel);
			if(matcher.matches()) {
				bodyData.append(CommonUtil.byteFiller("", 3));
				bodyData.append(CommonUtil.zerofill(matcher.group(1), 4));
				bodyData.append(CommonUtil.zerofill(matcher.group(2), 4));
			//이 외의 경우 빈값으로 세팅
			} else {
				bodyData.append(CommonUtil.byteFiller("", 11));
			}
		}
	}

	public static void main(String[] args) {
		new GalaxiaDiffUpload();
	}
}
