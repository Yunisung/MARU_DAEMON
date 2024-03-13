package com.pgmate.dm.main;

import com.pgmate.dm.dao.WelcomeDiffUploadDAO;
import com.pgmate.dm.util.SFTPUtil;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeDiffUpload {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private SmsGw smsGw = null;
    private String msgBody = "";

    // TEST SFTP SERVER
	private static String HOST = "118.130.130.27";
    // LIVE SFTP SERVER
//    private static String HOST = "118.129.171.153";
    private static int PORT = 5555;

    //WELCOME SFTP SERVER
    private static String userId = "bkwinners";

//    private static String MCHT_PATH="D:\\welcome\\diffMcht\\";
//    private static String SETTLE_PATH="D:\\welcome\\diffSettle\\";
    private static String MCHT_PATH="/home/bkwinners/diff/mcht_welcome";
    private static String SETTLE_PATH="/home/bkwinners/diff/settle_welcome";

//    /upload/dfsttm/send (가맹점 요청파일)
//    /upload/dfsttm/recv (내부검증 및 카드사 결과파일)
    private static String WELCOME_UPLOAD_PATH ="/upload/dfsttm/send";		//테스트 폴더
    //	private static String GALAXIA_UPLOAD_PATH="/request";	//운영 폴더

    private String nowDate = "";
    private int dataCnt = 0;
    private long dataAmt = 0;
    private int rowCnt = 0;

    private String identity = "6758600152";

    // 하위사업자 등록 카드사 리스트
    private String[] cardCdList = {"01", "03", "04", "06", "11", "12", "14", "16", "44"};

    public WelcomeDiffUpload() {
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
        logger.info("========== WELCOME 하위사업자 등록 START ==========");

        nowDate = CommonUtil.getCurrentDate("yyyyMMdd");

        String uploadPath = MCHT_PATH + nowDate.substring(0, 6);
        //merc_welcome_사업자번호_YYYYMMDD(요청일자)_req
        String fileName = "merc_welcome_" + identity + "_" + nowDate + "_req";

        WelcomeDiffUploadDAO dao = new WelcomeDiffUploadDAO();

        File folder = new File(uploadPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        final SFTPUtil sftpUtil = new SFTPUtil();
        try {

            //SFTP 서버 접속
            sftpUtil.init(HOST, userId, "", PORT);

            //파일명 생성
            uploadPath += File.separator + fileName;

            logger.info("DIFF MCHT UPLOAD FILE NAME ===> {}", uploadPath);

            //파일 객체 생성
            File uploadFile = new File(uploadPath);

            //bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile), "euc-kr"));
            fos = new FileOutputStream(uploadFile);
            osw = new OutputStreamWriter(fos, "euc-kr");
            bw = new BufferedWriter(osw);

            List<SharedMap<String, Object>> mchtList = dao.getMchtList();

            logger.info("WELCOME DIFFMCHT DATA SETTING START");
            headerMchtSetting(bw);
            dataMchtSetting(bw, mchtList, cardCdList, dao);
            totalMchtSetting(bw);
            bw.flush();
            logger.info("WELCOME DIFFMCHT DATA SETTING END");

            //WELCOME 파일 업로드
            if (sftpUtil.upload(WELCOME_UPLOAD_PATH, uploadFile)) {
                logger.info("===== WELCOME DIFFMCHT UPLOAD SUCCESSS =====");
                if (mchtList.size() > 0) {
                    dao.updateDiffMcht(mchtList);
                }
            } else {
                logger.info("===== WELCOME DIFFMCHT UPLOAD FAIL =====");
            }
        } catch (Exception e) {
            logger.error("UPLOAD MCHT DIFF ERROR ===> {}", e.getMessage());
            msgBody = "웰컴 영중소 가맹점 업로드 오류. 확인요망 [" + e.getMessage() + "]";
            smsGw.sendMessage("0", "4", msgBody);
        } finally {
            sftpUtil.disconnection();
            try {
                if(bw != null) bw.close();
                if(osw != null) osw.close();
                if(fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("========== WELCOME 하위사업자 등록 END ==========");
    }

    /**
     * 하위사업자 등록 해더전문
     *
     * @param bw
     * @throws IOException
     */
    private void headerMchtSetting(BufferedWriter bw) throws IOException {
        StringBuffer headerData = new StringBuffer();

        headerData.append(CommonUtil.byteFiller("10", 2));	//레코드 구분
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
     * @param cardCdList
     * @param dao
     * @throws IOException
     */
    private void dataMchtSetting(BufferedWriter bw, List<SharedMap<String, Object>> mchtList, String[] cardCdList, WelcomeDiffUploadDAO dao) throws IOException {
        StringBuffer bodyData = new StringBuffer();
        int lastSeq = 0;

        for(SharedMap<String, Object> map : mchtList) {
            // 카드사별 각각 업로드 / 가맹점 1 : 카드사 N
            for(String cardCd : this.cardCdList) {
                // 시퀀스 값 설정(+1)
                if(lastSeq == 0) {
                    lastSeq = dao.getLastSeq();
                }

                lastSeq += 1;

                String seq = "WELCOME" + CommonUtil.zerofill(String.valueOf(lastSeq), 13);

                bodyData = new StringBuffer();

                bodyData.append(CommonUtil.byteFiller("11", 2));	//레코드구분
                bodyData.append(CommonUtil.zerofill(00, 2));	//등록구분
                bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//오픈마켓 사업자번호
                bodyData.append(CommonUtil.zerofill(cardCd, 2));	//카드사코드
                bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo").replace("-", ""), 10));	//사업자등록번호(하위몰)
                bodyData.append(getRPad(map.getString("bizType"), 20, " "));	//업종명(하위몰)
                bodyData.append(getRPad(map.getString("mchtName"), 40, " "));	//회사명(하위몰)
                bodyData.append(getRPad(map.getString("mchtUrl"), 80, " "));	//웹사이트URL(하위몰)
                bodyData.append(getRPad(map.getString("addr1") + map.getString("addr2"), 100, " "));	//주소(하위몰)
                bodyData.append(CommonUtil.zerofill(map.getString("zip"), 6));	//우편번호
                bodyData.append(getRPad(map.getString("mchtCeo"), 40, " "));	//대표자명(하위몰)
                cutAndSetTel(bodyData, map.getString("mchtPhone"));	//전화번호(하위몰)
                bodyData.append(getRPad(map.getString("mchtEmail"), 40, " "));	//이메일(하위몰)
                bodyData.append(getRPad(nowDate, 8, " "));	//정보등록일
                bodyData.append(getRPad(seq, 20, " ")); // 시퀀스
                bodyData.append(CommonUtil.byteFiller("", 109));    // 공백 109

                bw.write(bodyData.toString());
                bw.newLine();

                dataCnt++;
            }
        }
        logger.info("WELCOME DIFFMCHT {} DATA", dataCnt);
    }

    /**
     * 하위사업자 등록 토탈전문
     *
     * @param bw
     * @throws IOException
     */
    public void totalMchtSetting(BufferedWriter bw) throws IOException {
        StringBuffer totalData = new StringBuffer();

        totalData.append(CommonUtil.byteFiller("30", 2));	//레코드구분
        totalData.append(CommonUtil.zerofill(dataCnt, 10));	//총 전송 건수
        totalData.append(CommonUtil.byteFiller("", 488));	//공백

        bw.write(totalData.toString());

        //초기화
        dataCnt = 0;
    }

    /**
     * 영중소 가맹점 우대수수료 차액정산
     *
     * @throws IOException
     */
    public void makeDiffSettle() {
        logger.info("========== WELCOME 차액정산 등록 START ==========");

        nowDate = CommonUtil.getCurrentDate("yyyyMMdd");

        String uploadPath = SETTLE_PATH + nowDate.substring(0, 6);
        String fileName = "daff_welcome_" + identity + "_" + nowDate + "_req";

        File folder = new File(uploadPath);
        if(!folder.exists()) {
            folder.mkdir();
        }

        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;

        final SFTPUtil sftpUtil = new SFTPUtil();
        try {
            //SFTP 서버 접속
            sftpUtil.init(HOST, userId, "", PORT);

            uploadPath += File.separator + fileName;
            logger.info("DIFF SETTLE UPLOAD FILE NAME ===> {}", uploadPath);

            WelcomeDiffUploadDAO dao = new WelcomeDiffUploadDAO();

            //파일 객체 생성
            File uploadFile = new File(uploadPath);

            fos = new FileOutputStream(uploadFile);
            osw = new OutputStreamWriter(fos, "euc-kr");
            bw = new BufferedWriter(osw);

            List<SharedMap<String, Object>> midList = dao.getMidList();

            logger.info("WELCOME DIFFSETTLE START DATA SETTING START");
            startDiffSetting(bw);

            for (SharedMap<String, Object> midMap : midList) {
                String mid = midMap.getString("vanId");
                List<SharedMap<String, Object>> payList = dao.getPayList(mid);
                List<SharedMap<String, Object>> rfdList = dao.getRfdList(mid);

                List<SharedMap<String, Object>> rootTrxList = dao.getRootTrxList(mid);
                List<SharedMap<String, Object>> partialList = new ArrayList<SharedMap<String,Object>>();

                for(SharedMap<String, Object> map : rootTrxList) {
                    int rfdTurn;
                    String lastRfdTurn = dao.getLastRfdTurn(map.getString("rootTrxId"));
                    if(!lastRfdTurn.equals("")) {
                        rfdTurn = Integer.parseInt(lastRfdTurn) + 1;
                    } else {
                        rfdTurn = 2;
                    }

                    List<SharedMap<String, Object>> partialTrxList = dao.getPartialTrx(map.getString("rootTrxId"));
                    for(SharedMap<String, Object> partialTrxMap : partialTrxList) {
                        partialTrxMap.put("rfdTurn", rfdTurn);
                        partialList.add(partialTrxMap);
                        rfdTurn++;
                    }
                }

                logger.info("WELCOME DIFFSETTLE DATA SETTING START");
                headerDiffSetting(bw, mid);
                if(payList.size() > 0 || rfdList.size() > 0 || partialList.size() > 0) {
                    dataDiffSetting(bw, payList, rfdList, partialList);
                }
                totalDiffSetting(bw);
                logger.info("WELCOME DIFFSETTLE DATA SETTING END");

                //WELCOME 파일 업로드
                if(sftpUtil.upload(WELCOME_UPLOAD_PATH, uploadFile)){
                    logger.info("===== WELCOME DIFF SETTLE UPLOAD SUCCESSS =====");
                    //----------------> 확인 필요
                    if(payList.size() > 0) {
                        dao.insertTrxDiffUpload(payList, nowDate);
                    }
                    if(rfdList.size() > 0) {
                        dao.insertTrxDiffUpload(rfdList, nowDate);
                    }
                    if(partialList.size() > 0) {
                        dao.insertTrxDiffUpload(partialList, nowDate);
                    }
                } else {
                    logger.info("===== WELCOME DIFFSETTLE UPLOAD FAIL =====");
                }
            }

            endDiffSetting(bw);
            bw.close();

        } catch (Exception e) {
            logger.error("MAKE DIFF SETTLE ERROR ===> {}", e.getMessage(), e);
            msgBody = "웰컴 차액정산 업로드 오류. 확인요망 [" + e.getMessage() + "]";
            smsGw.sendMessage("0", "4", msgBody);
        } finally {
            sftpUtil.disconnection();
            try {
                if(bw != null) bw.close();
                if(osw != null) osw.close();
                if(fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("===== WELCOME DIFFSETTLE UPLOAD END =====");
    }

    /**
     * 차액정산 등록 START 전문
     *
     * @param bw
     * @throws IOException
     */
    private void startDiffSetting(BufferedWriter bw) throws IOException {
        StringBuffer startData = new StringBuffer();

        startData.append(CommonUtil.byteFiller("01", 2));	//레코드구분
        startData.append(CommonUtil.zerofill(nowDate, 8));	//파일생성일자
        startData.append(CommonUtil.byteFiller(identity, 10));	//사업자번호
        startData.append(CommonUtil.byteFiller("WELCOMEPAY", 10));	//PG사 구분
        startData.append(CommonUtil.byteFiller("", 320));	//공백

        bw.write(startData.toString());
        bw.newLine();

        rowCnt ++;
    }

    /**
     * 차액정산 등록 헤더전문
     *
     * @param bw
     * @param mid
     * @throws IOException
     */
    private void headerDiffSetting(BufferedWriter bw, String mid) throws IOException {
        StringBuffer headerData = new StringBuffer();

        headerData.append(CommonUtil.byteFiller("10", 2));	//레코드구분
        headerData.append(CommonUtil.byteFiller(mid, 10));	//mid 구분
        headerData.append(CommonUtil.byteFiller("", 338));	//공백

        bw.write(headerData.toString());
        bw.newLine();

        rowCnt ++;
    }

    /**
     * 차액정산 등록 데이터전문
     *
     * @param bw
     * @param payList
     * @param rfdList
     * @throws IOException
     */
    private void dataDiffSetting(BufferedWriter bw, List<SharedMap<String, Object>> payList, List<SharedMap<String, Object>> rfdList, List<SharedMap<String, Object>> partialList) throws IOException {
        StringBuffer bodyData = new StringBuffer();

        for (SharedMap<String, Object> map : payList){
            bodyData = new StringBuffer();

            bodyData.append(CommonUtil.byteFiller("11", 2));	//레코드구분
            bodyData.append(CommonUtil.zerofill("0", 1));		//매입취소구분
            bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//거래일자
            bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//중간하위사업자번호
            bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo"), 10));	//최종하위사업자번호
            bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 40));	//PG거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 40));	//PG원거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("trxId"), 64));	//가맹점 주문번호
            bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//하위사업자 매출액
            bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//원거래 매입금액
            bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//승인일자
            bodyData.append(CommonUtil.byteFiller("", 40));	//가맹점 예약 필드
            bodyData.append(CommonUtil.byteFiller("", 97));	//가맹점 검증값

            logger.info("WELCOME PAY SETTLE DATA FILE : [" + bodyData.toString() + "]");

            bw.write(bodyData.toString());
            bw.newLine();

            dataCnt++;
            rowCnt ++;
            dataAmt = dataAmt + map.getLong("amount");
        }

        for (SharedMap<String, Object> map : rfdList){
            bodyData = new StringBuffer();

            bodyData.append(CommonUtil.byteFiller("11", 2));	//레코드구분
            bodyData.append(CommonUtil.zerofill("1", 1));		//매입취소구분
            bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//거래일자
            bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//중간하위사업자번호
            bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo"), 10));	//최종하위사업자번호
            bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 40));	//PG거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("rootVanTrxId"), 40));	//PG원거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("trxId"), 64));	//가맹점 주문번호
            bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//하위사업자 매출액
            bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//원거래 매입금액
            bodyData.append(CommonUtil.zerofill(map.getString("rootTrxDay"), 8));	//원거래 승인일자
            bodyData.append(CommonUtil.byteFiller("", 40));	//가맹점 검증값
            bodyData.append(CommonUtil.byteFiller("", 97));	//가맹점 검증값

            logger.info("WELCOME REFUND SETTLE DATA FILE : [" + bodyData.toString() + "]");

            bw.write(bodyData.toString());
            bw.newLine();

            dataCnt++;
            rowCnt ++;
            dataAmt = dataAmt - map.getLong("amount");
        }

        for (SharedMap<String, Object> map : partialList){
            bodyData = new StringBuffer();

            bodyData.append(CommonUtil.byteFiller("11", 2));	//레코드구분
            bodyData.append(CommonUtil.zerofill("1", 1));		//매입취소구분
            bodyData.append(CommonUtil.zerofill(map.getString("trxDay"), 8));	//거래일자
            bodyData.append(CommonUtil.zerofill(map.getString("compNo"), 10));	//중간하위사업자번호
            bodyData.append(CommonUtil.zerofill(map.getString("mchtCompNo"), 10));	//최종하위사업자번호
            bodyData.append(CommonUtil.byteFiller(map.getString("vanTrxId"), 40));	//PG거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("rootVanTrxId"), 40));	//PG원거래번호
            bodyData.append(CommonUtil.byteFiller(map.getString("trxId"), 64));	//가맹점 주문번호
            bodyData.append(CommonUtil.zerofill(map.getString("amount"), 15));	//하위사업자 매출액
            bodyData.append(CommonUtil.zerofill(map.getString("rootAmount"), 15));	//원거래 매입금액
            bodyData.append(CommonUtil.zerofill(map.getString("rootTrxDay"), 8));	//원거래 승인일자
            bodyData.append(CommonUtil.byteFiller("", 40));	//가맹점 검증값
            bodyData.append(CommonUtil.byteFiller("", 97));	//가맹점 검증값

            logger.info("WELCOME PARIAL REFUND SETTLE DATA FILE : [" + bodyData.toString() + "]");

            bw.write(bodyData.toString());
            bw.newLine();

            dataCnt++;
            rowCnt ++;
            dataAmt = dataAmt - map.getLong("amount");
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

        totalData.append(CommonUtil.byteFiller("12", 2));	//레코드구분
        totalData.append(CommonUtil.zerofill(dataCnt, 7));		//총 건수 합계
        totalData.append(CommonUtil.zerofill(dataAmt, 18));	//총 매출액 합계
        totalData.append(CommonUtil.byteFiller("", 323));

        bw.write(totalData.toString());

        rowCnt ++;
    }

    /**
     * 차액정산 등록 END 전문
     *
     * @param bw
     * @throws IOException
     */
    private void endDiffSetting(BufferedWriter bw) throws IOException {
        StringBuffer startData = new StringBuffer();

        rowCnt ++;
        startData.append(CommonUtil.byteFiller("02", 2));	//레코드구분
        startData.append(CommonUtil.zerofill(rowCnt, 7));	//전체 줄 수
        startData.append(CommonUtil.byteFiller("", 341));	//공백

        bw.write(startData.toString());
        bw.newLine();
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
        new WelcomeDiffUpload();
    }
}
