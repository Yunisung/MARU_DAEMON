package com.pgmate.dm.main;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.dm.dao.ChargeSettleReserveDAO;
import com.pgmate.dm.dao.FirmFailCheckDAO;
import com.pgmate.dm.util.FirmClient;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ChargeSettleReservePayOut {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettleReservePayOut.class );
    private SmsGw smsGw = null;
    private String msgBody = "";

//    private String firmServer = "10.100.200.10";
    private String firmServer = "pgwas3";
    private String compNm = "";
//    private int firmPort = 10006;
    private int firmPort = 10026;
    private int firmTimeOut = 70000;
    private int firmStartTime = 3000; //출금 시작 시간
    private int firmEndTime = 233000; //출금 중지 시간

    public static void main(String[] args){
        new ChargeSettleReservePayOut();
    }

    public ChargeSettleReservePayOut() {
        logger.info("==================================================");
        logger.info("ChargeSettleReservePayOut Strart");
        smsGw = new SmsGw();

        configSetting();
        chargeSettleReserveFirm();

        logger.info("ChargeSettleReservePayOut End");
        logger.info("==================================================");
    }

    public void chargeSettleReserveFirm() {
        ChargeSettleReserveDAO dao = new ChargeSettleReserveDAO();

        //거래건 불러오기
        try {
            int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));

            //매일 23:30~00:30분까지는 은행 점검시간이라서 출금기능 막음
            if (currentTime > firmEndTime || currentTime < firmStartTime) {
                logger.info("- -- --- ---- ---- ---- 출금 서비스 가능한 시간이 아닙니다. ---- ---- ---- --- -- -");
                return;
            }

            List<SharedMap<String,Object>> chargeSettleList = new ArrayList<>();
            List<SharedMap<String,Object>> reserveList = dao.getChareSettleReserveList();
            List<SharedMap<String,Object>> realTimeList = dao.getChareSettleRealTimeList();

            if(reserveList.size() > 0) {
                for(SharedMap<String, Object> data : reserveList) {
                    chargeSettleList.add(data);
                }
            }

            if(realTimeList.size() > 0) {
                for(SharedMap<String, Object> data : realTimeList) {
                    chargeSettleList.add(data);
                }
            }


            logger.info("chargeSettleReservePayOut COUNT : {}", chargeSettleList.size());

            if(chargeSettleList.size() > 0) {
                logger.info("==================================================");
                logger.info("충전정산 예약 START");

                for(SharedMap<String,Object> data : chargeSettleList) {
                    msgBody = "";
                    FirmBean firmBean = new FirmBean();
                    String idx = "";
                    boolean errFlag = false;
                    String status = "실패";
                    int errCnt = 0;

                    //상태값 전송으로 변경
                    dao.updateStatus(data.getString("trxId"));

                    //펌뱅킹 광주은행으로 고정
                    String vactBankCd = "034";
                    SharedMap<String, Object> chargeMngMap = dao.getMchtChargeMng(data.getString("mchtId"));

                    // 월세앱 정보
                    SharedMap<String, Object> rentMap = dao.getMchtRent(data.getString("mchtId"));
                    SharedMap<String, Object> trxCap = dao.getTrxCapById(data.getString("refTrxId"));
                    boolean isRentService = !CommonUtil.isNullOrSpace(rentMap.getString("mchtSettleNotiAddr"));
                    String hookAddr = isRentService ? rentMap.getString("mchtSettleNotiAddr") : chargeMngMap.getString("hookAddr");

                    // 잔액체크
                    long balance = dao.getMchtBalance(data.getString("mchtId")).getLong("balance");

                    long fee = chargeMngMap.getLong("withdrawFee");
                    long feeVat = calcVat(fee);
                    long transferLimit = chargeMngMap.getLong("transferLimit");

                    long netAmount = data.getLong("netAmount");
                    long netAmountWithFee = data.getLong("netAmount") + fee + feeVat;
                    long checkAmount = netAmountWithFee + transferLimit;

                    if(balance >= checkAmount) {
                        if("0".equals(data.getString("retry"))) {
                            //첫시도
                            firmBean = new FirmClient(firmServer, firmPort, firmTimeOut).transfer(vactBankCd, data.getString("bankCd"), data.getString("decAccount").replace("-", "").trim(), data.getLong("amount"), data.getString("trxId"), compNm, "CS");
//                            firmBean.resultCd = "0000";
//                            firmBean.resultMsg = "정상";
//                            firmBean.idx = 9999;

                            idx = String.valueOf(firmBean.idx);

                            if("".equals(idx) || "0".equals(idx)) {
                                idx = dao.getIdx(data.getString("trxId"));

                                logger.info("idx 재검색 : [{}][{}]", data.getString("trxId"), idx);
                            }

                            if(!firmBean.resultCd.equals("0000") ) {
                                msgBody = "충전정산 예약 출금 실패. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
                                logger.info(msgBody);
                                errFlag = true;
                            }else {
                                status = "완료";
                                msgBody = "충전정산 예약 출금 성공. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
                                logger.info(msgBody);

                                insertChargeSettleAndStlComplete(dao, data);

                                logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
                                runChargeSettleHook(trxCap, data, isRentService, hookAddr, firmBean, "출금", "출금완료");
                            }

                            if(!dao.updateRefIdUpdate(data.getString("trxId"), idx)) {
                                msgBody = "PG_CHARGE_SETTLE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                                logger.info(msgBody);
                            }

                            if(!dao.updateRefIdUpdate2(data.getString("trxId"), idx)) {
                                msgBody = "PG_CHARGE_SETTLE_FIRM_RESERVE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                                logger.info(msgBody);
                            }

                        } else {
                            //재시도
                            String orgSeq = "";

                            orgSeq = dao.getSeqNo(data.getString("trxId"));

                            logger.info("충전정산 예약 결과확인 출금 : [{}][{}][{}]", data.getString("trxId"), data.getString("retry"), orgSeq);

                            firmBean = new FirmClient(firmServer, firmPort, firmTimeOut).resultCheck(vactBankCd, orgSeq);

                            if(vactBankCd.equals("034")) {
                                //광주은행일때
                                if(firmBean.resultCd.equals("0000")) {
                                    status = "완료";
                                    msgBody = "충전정산 예약 결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
                                    logger.info(msgBody);

                                    insertChargeSettleAndStlComplete(dao, data);

                                    logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
                                    runChargeSettleHook(trxCap, data, isRentService, hookAddr, firmBean, "출금", "출금완료");

                                }else if(firmBean.resultCd.equals("VTIM") || firmBean.resultCd.equals("0011")) {
                                    logger.info("더즌 타임아웃, 이중송금방지, 한번더 실행");

                                }else {
                                    msgBody = "충전정산 예약 결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
                                    logger.info(msgBody);
                                    errFlag = true;
                                }

                            } else {
                                if( firmBean.resultCd.equals("KS10")) {
                                    logger.info("충전정산 예약 출금 재시도 : [{}]", data.getString("trxId"));
                                    firmBean = new FirmClient(firmServer, firmPort, firmTimeOut).reTransfer(vactBankCd, data.getString("trxId"));
                                    logger.info("결과메세지 체크 : {}", firmBean.resultMsg);
                                    //PYS : 결과메세지 제대로 나오면 아래 로직은 삭제하는걸로.
                                    if(firmBean.resultCd.startsWith("KS")) {
                                        firmBean.resultMsg = FirmFailCheckDAO.getResultMsg("ERR", firmBean.resultCd);
                                    } else {
                                        firmBean.resultMsg = FirmFailCheckDAO.getResultMsg(firmBean.bankCd, firmBean.resultCd);
                                    }

                                    //위에 있는 출금로직 복붙
                                    idx = String.valueOf(firmBean.idx);

                                    if("".equals(idx) || "0".equals(idx)) {
                                        idx = dao.getIdx(data.getString("trxId"));

                                        logger.info("idx 재검색 : [{}][{}]", data.getString("trxId"), idx);
                                    }

                                    if(!firmBean.resultCd.equals("0000") ) {
                                        msgBody = "충전정산 예약 재시도 출금 실패. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
                                        logger.info(msgBody);
                                        errFlag = true;
                                    }else {
                                        status = "완료";
                                        msgBody = "충전정산 예약 재시도 출금 성공. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
                                        logger.info(msgBody);

                                        insertChargeSettleAndStlComplete(dao, data);

                                        logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
                                        runChargeSettleHook(trxCap, data, isRentService, hookAddr, firmBean, "출금", "출금완료");
                                    }

                                    if(!dao.updateRefIdUpdate(data.getString("trxId"), idx)) {
                                        msgBody = "PG_CHARGE_SETTLE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                                        logger.info(msgBody);
                                    }

                                    if(!dao.updateRefIdUpdate2(data.getString("trxId"), idx)) {
                                        msgBody = "PG_CHARGE_SETTLE_FIRM_RESERVE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                                        logger.info(msgBody);
                                    }
                                }else {
                                    //PYS : KS10이 아닐땐 기존 로직실행
                                    if(!firmBean.resultCd.equals("0000") ) {
                                        msgBody = "충전정산 예약 결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
                                        logger.info(msgBody);
                                        errFlag = true;
                                    }else {
                                        status = "완료";
                                        msgBody = "충전정산 예약 결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
                                        logger.info(msgBody);

                                        insertChargeSettleAndStlComplete(dao, data);

                                        logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
                                        runChargeSettleHook(trxCap, data, isRentService, hookAddr, firmBean, "출금", "출금완료");
                                    }
                                }
                            }
                        }
                    } else {
                        msgBody = "충전정산 잔액부족. trxId : [" + data.getString("trxId") + "], mchtId : [" + data.getString("mchtId") + "]";
                        logger.info(msgBody);
                        errFlag = true;
                    }

                    String day = CommonUtil.getCurrentDate("yyyyMMdd");
                    String time = CommonUtil.getCurrentDate("HHmmss");

                    if(!dao.updatePayOutRes(data.getString("trxId"), (balance - netAmount), firmBean.resultCd, firmBean.resultMsg, status, day, time)) {
                        msgBody = "PG_CHARGE_SETTLE_FIRM_RESERVE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                        logger.info(msgBody);
//                        smsGw.sendMessage("0", "4", msgBody);
                    }

                    // 집계시 하위 거래건 결과 update
                    if("집계".equals(data.getString("trxType"))) {
                        if(!dao.updatePayOutResChild(data.getString("trxId"), (balance - netAmount), firmBean.resultCd, firmBean.resultMsg, status, day, time)) {
                            msgBody = "하위 PG_CHARGE_SETTLE_FIRM_RESERVE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

                            logger.info(msgBody);
                        }
                    }

                    if(errFlag) {
                        String sendCnt = dao.getRetry(data.getString("trxId"));

                        //출금 실패시 출금 전송 횟수가 3회일 경우 SMS 알림 발송
                        if(!"".equals(sendCnt)) {
                            errCnt = Integer.parseInt(sendCnt);

                            //3회 실패 시
                            if(errCnt == 3) {
                                //230622 타행이체불능 에러 처리 안되어있을 때 로직 수행
                                if(dao.getChargeErrCount(data.getString("trxId")) == 0) {
                                    //펌에러 테이블에 저장
                                    SharedMap<String, Object> errData = dao.getChargeSettle(data.getString("trxId"));
                                    errData.put("refId", data.getString("refId"));
                                    errData.put("resultCd", data.getString("resultCd"));
                                    errData.put("resultMsg", data.getString("resultMsg"));
                                    String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
                                    errData.put("regDay", regDate.substring(0, 8));
                                    dao.insertTrxErr(errData);

                                    /*if (!firmBean.resultCd.equals("XXXX") && !firmBean.resultCd.equals("")) {
                                        logger.info("===========================");
                                        logger.info("충전정산 예약 잔액 복구 로직 실행");
                                        logger.info("가맹점 ID : {}", data.getString("mchtId"));
                                        logger.info("복구금액 : {}", errData.getLong("netAmount"));
                                        logger.info("===========================");

                                        //실패거래건의 실출금액 조회
                                        long netAmt = errData.getLong("netAmount");
                                        //실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌
                                        dao.updateChargeSettleBalance(data.getString("trxId"), data.getString("mchtId"), netAmt);
                                        //실패건 PG_CHARGE_SETTLE 테이블에서 삭제
                                        dao.deleteChargeSettle(data.getString("trxId"));
                                    }*/

                                    logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
                                    runChargeSettleHook(trxCap, data, isRentService, hookAddr, firmBean, "출금실패", "출금실패");

                                    msgBody = "충전정산 예약 출금 " + errCnt + "회 실패. 확인요망 [" + data.getString("trxId") + "][" + firmBean.resultMsg + "]";

                                    logger.info(msgBody);
//                                    smsGw.sendMessage("0", "4", msgBody);
                                }

                            }
                        }
                    }

                }
                logger.info("충전정산 예약 출금 END");
                logger.info("==================================================");
            }
        } catch(Exception e) {
            logger.error(e.getMessage(), e);

            msgBody = "충전정산 예약출금 오류발생. 확인요망 [" + e.getMessage() + "]";
//            smsGw.sendMessage("0", "4", msgBody);
        }

    }

    private long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }

    private void insertChargeSettleAndStlComplete(ChargeSettleReserveDAO dao, SharedMap<String,Object> data) {
        SharedMap<String,Object> chargeSettleMap = createRefundChargeSettleMap(data);
        if(dao.insertChargeSettle(chargeSettleMap)) {
            //정산완료처리
            if(!"집계".equals(data.getString("trxType"))) {
                dao.updateTrxCapDtlStlComplete(data.getString("refTrxId"));
            } else {
                // 하위 거래건 정산완료 처리
                List<SharedMap<String,Object>> reserveChildList = dao.getChareSettleReserveChildList(data.getString("trxId"));
                for(SharedMap<String,Object> child : reserveChildList) {
                    dao.updateTrxCapDtlStlComplete(child.getString("refTrxId"));
                }
            }
        }
    }

    private SharedMap<String,Object> createRefundChargeSettleMap(SharedMap<String,Object> chargeSettleFirmMap) {
        SharedMap<String,Object> chargeSettleMap = new SharedMap<String,Object>();
        String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
        ChargeSettleReserveDAO dao = new ChargeSettleReserveDAO();

        chargeSettleMap.put("trxId"	    , chargeSettleFirmMap.getString("trxId"));
        chargeSettleMap.put("mchtId"	, chargeSettleFirmMap.getString("mchtId"));
        chargeSettleMap.put("trxType"	, "출금");
        chargeSettleMap.put("trxUnit"	, "펌뱅킹");
        chargeSettleMap.put("trxDay"	, regDate.substring(0, 8));
        chargeSettleMap.put("trxTime"	, regDate.substring(8));
        chargeSettleMap.put("amount"	, Math.abs(chargeSettleFirmMap.getLong("amount")));
        chargeSettleMap.put("fee"		, Math.abs(chargeSettleFirmMap.getLong("fee")));
        chargeSettleMap.put("feeVat"	, Math.abs(chargeSettleFirmMap.getLong("feeVat")));
        chargeSettleMap.put("bankFee"	, chargeSettleFirmMap.getLong("bankFee"));
        chargeSettleMap.put("netAmount" , Math.abs(chargeSettleFirmMap.getLong("netAmount")));
        chargeSettleMap.put("balance"	, dao.getMchtBalance(chargeSettleFirmMap.getString("mchtId")).getLong("balance")-Math.abs(chargeSettleFirmMap.getLong("netAmount")));
        chargeSettleMap.put("trackId"	, chargeSettleFirmMap.getString("trackId"));
        chargeSettleMap.put("refId"	    , chargeSettleFirmMap.getString("refId"));
        chargeSettleMap.put("bankCd"	, chargeSettleFirmMap.getString("bankCd"));
        chargeSettleMap.put("bankName"	, chargeSettleFirmMap.getString("bankName"));
        chargeSettleMap.put("account"	, chargeSettleFirmMap.getString("account"));
        chargeSettleMap.put("holder"	, chargeSettleFirmMap.getString("holder"));
        chargeSettleMap.put("recordInfo"	, chargeSettleFirmMap.getString("recordInfo"));
        chargeSettleMap.put("summary"	, "");
        chargeSettleMap.put("regId"	    , chargeSettleFirmMap.getString("mchtId"));
        chargeSettleMap.put("regDay"	, regDate.substring(0, 8));

        return chargeSettleMap;
    }


    private void runChargeSettleHook(SharedMap<String,Object> trxCap, SharedMap<String,Object> data,
                                     boolean isRentService, String hookAddr, FirmBean firmBean, String trxType, String status) {
        if(isRentService) {
            //가맹점 정산 처리
            if(!CommonUtil.isNullOrSpace(hookAddr)) {
                logger.info("가맹점 정산 노티");

                if("집계".equals(data.getString("trxType"))) {
                    // 집계 데이터라면 하위 예약이체건을 모두 가져와 노티 전송
                    ChargeSettleReserveDAO dao = new ChargeSettleReserveDAO();
                    List<SharedMap<String,Object>> reserveChildList = dao.getChareSettleReserveChildList(data.getString("trxId"));
                    for(SharedMap<String,Object> child : reserveChildList) {
                        SharedMap<String, Object> trxCapChild = dao.getTrxCapById(child.getString("refTrxId"));

                        child.put("mchtId", trxCapChild.getString("mchtId"));
                        child.put("mchtName", trxCapChild.getString("name"));
                        child.put("trxType", trxCapChild.getString("trxType"));
                        child.put("bankCd", child.getString("bankCd"));
                        child.put("bankName", child.getString("bankName"));
                        child.put("account", child.getString("account"));
                        child.put("stlAmount", trxCapChild.getString("stlAmount"));
                        child.put("billingType", trxCapChild.getString("billingType"));
                        child.put("accntHolder", child.getString("holder"));
                        child.put("sender", child.getString("recordInfo"));
                        child.put("authCd", trxCapChild.getString("authCd"));
                        child.put("trxId", child.getString("trxId"));
                        child.put("trackId", trxCapChild.getString("trackId"));

                        //출금 노티
                        String mchtSettle = setMchtSettle(child, status, firmBean.resultCd, firmBean.resultMsg);
                        child.put("payLoad", mchtSettle);
                        child.put("trxType", trxType);
                        new ChargeSettleHook(hookAddr, child, "0").start();
                    }

                } else {
                    logger.info("가맹점 정산 노티");

                    data.put("mchtName", trxCap.getString("name"));
                    data.put("stlAmount", trxCap.getString("stlAmount"));
                    data.put("amount", trxCap.getString("amount"));
                    data.put("billingType", trxCap.getString("billingType"));
                    data.put("accntHolder", data.getString("holder"));
                    data.put("sender", data.getString("recordInfo"));
                    data.put("authCd", trxCap.getString("authCd"));
                    data.put("trxId", data.getString("trxId"));
                    data.put("trackId", trxCap.getString("trackId"));

                    //출금 노티
                    String mchtSettle = setMchtSettle(data, status, firmBean.resultCd, firmBean.resultMsg);
                    data.put("payLoad", mchtSettle);
                    data.put("trxType", trxType);
                    new ChargeSettleHook(hookAddr, data, "0").start();
                }
            }
        } else {
            // 출금완료 결과 noti 발송
            if(!CommonUtil.isNullOrSpace(hookAddr)) {
                String payLoad = setPayLoad(data, status, firmBean.resultCd, firmBean.resultMsg);
                data.put("payLoad", payLoad);
                data.put("trxType", trxType);
                new ChargeSettleHook(hookAddr, data, "0").start();
            }
        }
    }

    public void configSetting() {
        try{
            // 프로퍼티 파일 위치
            //운영
//            String propFile = "/home/bkwinners/MARU_DAEMON/conf/firmconfig.properties";
            //테스트
            String propFile = "/var/lib/jenkins/MARU_DAEMON/conf/firmconfig.properties";

            // 프로퍼티 객체 생성
            Properties props = new Properties();

            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));

            // 항목 읽기
            firmServer = props.getProperty("firm_server");

            String strFirmPort = props.getProperty("firm_port");
            String strFirmTimeOut = props.getProperty("firm_timeout");
            String strStartTime = props.getProperty("firm_starttime");
            String strEndTime = props.getProperty("firm_endtime");

            if(strFirmPort != null && !"".equals(strFirmPort)) {
                firmPort = Integer.parseInt(strFirmPort);
            }

            if(strFirmTimeOut != null && !"".equals(strFirmTimeOut)) {
                firmTimeOut = Integer.parseInt(strFirmTimeOut);
            }

            if(strStartTime != null && !"".equals(strStartTime)) {
                firmStartTime = Integer.parseInt(strStartTime);
            }

            if(strEndTime != null && !"".equals(strEndTime)) {
                firmEndTime = Integer.parseInt(strEndTime);
            }
        }catch(Exception e){
            logger.info(e.getMessage(), e);
        }
    }

    public String setPayLoad(SharedMap<String, Object> sharedMap, String status, String resultCd, String resultMsg){
        SharedMap<String, String> payLoadMap = new SharedMap<String, String>();
        payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
        payLoadMap.put("trxId",sharedMap.getString("trxId"));
        payLoadMap.put("trxDay",CommonUtil.getCurrentDate("yyyyMMdd"));
        payLoadMap.put("trxTime",CommonUtil.getCurrentDate("HHmmss"));
        payLoadMap.put("status",status);
        payLoadMap.put("trackId",sharedMap.getString("trackId"));
        payLoadMap.put("resultCd",resultCd);
        payLoadMap.put("resultMsg",resultMsg);
        payLoadMap.put("amount",sharedMap.getString("amount"));
        String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
        return payLoad;
    }

    public String setMchtSettle(SharedMap<String,Object> sharedMap, String status, String resultCd, String resultMsg) {
        SharedMap<String, String> mchtSettleMap = new SharedMap<>();
        mchtSettleMap.put("mchtId", sharedMap.getString("mchtId"));
        mchtSettleMap.put("trxId",sharedMap.getString("trxId"));
        mchtSettleMap.put("trackId",sharedMap.getString("trackId"));
        mchtSettleMap.put("mchtName", sharedMap.getString("mchtName"));
        mchtSettleMap.put("amount", sharedMap.getString("amount"));
        mchtSettleMap.put("stlAmount", sharedMap.getString("stlAmount"));
        mchtSettleMap.put("trxType", sharedMap.getString("trxType"));
        mchtSettleMap.put("status",status);
        mchtSettleMap.put("billingType", sharedMap.getString("billingType"));
        mchtSettleMap.put("resultCd", resultCd);
        mchtSettleMap.put("resultMsg", resultMsg);
        mchtSettleMap.put("stlDay", CommonUtil.getCurrentDate("yyyyMMdd"));
        mchtSettleMap.put("payOutDate", CommonUtil.getCurrentDate("yyyyMMdd"));
        mchtSettleMap.put("accntHolder", sharedMap.getString("holder"));
        mchtSettleMap.put("sender", sharedMap.getString("recordInfo"));
        mchtSettleMap.put("bankCd", sharedMap.getString("bankCd"));
        mchtSettleMap.put("bankName", sharedMap.getString("bankName"));
        mchtSettleMap.put("account", sharedMap.getString("account"));
        mchtSettleMap.put("authCd", sharedMap.getString("authCd"));

        String mchtSettle = CommonUtil.toQueryString(mchtSettleMap, "UTF-8");
        logger.info("payload : {}", mchtSettle);
        return mchtSettle;
    }
}
