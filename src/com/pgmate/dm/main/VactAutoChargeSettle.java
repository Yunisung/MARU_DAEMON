package com.pgmate.dm.main;

import com.pgmate.dm.dao.SettleVactMchtDAO;
import com.pgmate.dm.dao.VactAutoChargeSettleDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CreateBy PYS
 * 가상계좌 자동충전정산 정산
 */

public class VactAutoChargeSettle {
    private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.VactAutoChargeSettle.class);
    private SmsGw smsGw = null;

    private String day = "";
    private String msgBody = "";
    private int dayErr = 0;
    private int daySumCnt = 0;
    private long daySumAmt = 0;

    public static void main(String[] args) { new VactAutoChargeSettle(); }

    public VactAutoChargeSettle() {
        String cmd = CommonUtil.getCurrentDate("yyyyMMdd");
        smsGw = new SmsGw();
        DecimalFormat formatter = new DecimalFormat("###,###");

        vactAutoChargeSettleExcute(cmd);
        vactAutoChargePayOut(cmd);

        day = cmd.substring(0,4) + "년 " + cmd.substring(4,6) + "월 " + cmd.substring(6) + "일";

        if(dayErr == 0) {
            msgBody = "[" + day + "] 자동충전정산 대상 건수 : " +  formatter.format(daySumCnt) + "건, 지급금액 : " + formatter.format(daySumAmt) + "원 입니다.";
        } else {
            msgBody = day + " 일 자동충전정산 오류. 확인요망.";
            //smsGw.sendMessage("0", "3", msgBody);
        }

        logger.info(msgBody);

    }

    public void vactAutoChargeSettleExcute(String stlDay) {
        try {

            logger.info("= == === === === === {} 일자 가상계좌 자동충전정산 시작 === === === === == =", stlDay);
            VactAutoChargeSettleDAO dao = new VactAutoChargeSettleDAO();
            List<SharedMap<String,Object>> settleList = dao.getAutoChargeSettleList(stlDay);
            logger.info("MAKE VACT AUTO CHARGE SETTLE COUNT : {}", settleList.size());

            for(SharedMap<String,Object> data : settleList) {

                String mchtId = data.getString("mchtId");
                String stlType = data.getString("stlType");
                String stlId = dao.getSettleId();

                SharedMap<String,Object> mchtMngVactMap	= dao.getMchtMngVactByMchtId(mchtId);
                //PG_CHARGE_SETTLE_AUTO에 넣을 데이터 세팅

                SharedMap<String,Object> settleData = new SharedMap<>();

                settleData.put("stlId", stlId);
                settleData.put("mchtId", mchtId);
                settleData.put("status", "지급대기");
                settleData.put("stlDay", stlDay);
                settleData.put("payType", "V");
                settleData.put("startDay", data.getString("startDay"));
                settleData.put("endDay", data.getString("endDay"));

                settleData.put("payAmt", data.getLong("payAmt"));
                settleData.put("payFee", data.getLong("payFee"));
                settleData.put("payVat", data.getLong("payFeeVat"));
                settleData.put("payCnt", data.getLong("payCnt"));

                settleData.put("rfdAmt", data.getLong("rfdAmt"));
                settleData.put("rfdFee", data.getLong("rfdFee"));
                settleData.put("rfdVat", data.getLong("rfdFeeVat"));
                settleData.put("rfdCnt", data.getLong("rfdCnt"));

                settleData.put("bankFee", data.getLong("vanFee"));
                settleData.put("stlAmount", data.getLong("stlAmount"));
                settleData.put("stlRate", mchtMngVactMap.getDouble("rate"));
                settleData.put("stlType", stlType);
                settleData.put("regId", "SYSTEM");



                logger.info("가상계좌 자동 충전정산 INSERT");
                logger.info("STL_ID  	: {}",stlId);
                logger.info("MEMBER_ID 	: {}",mchtId);
                logger.info("TAX ID 	: {}", CommonUtil.nToB(data.getString("taxId")));
                logger.info("STL_AMT 	: {}",data.getLong("stlAmount"));

                if(dao.insertSettleAuto(settleData)) {
                    dao.setSettleToIdx(stlId,stlDay,mchtId);
                    dao.updateTrxCap(stlId);

                    daySumAmt += data.getLong("stlAmount");
                    daySumCnt += data.getLong("payCnt");
                } else {
                    msgBody = "PG_CHARGE_SETTLE_AUTO VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
                    logger.info(msgBody);
                    dayErr++;
                }

                logger.info("==================================================");
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dayErr++;
        }
    }

    public void vactAutoChargePayOut(String stlDay) {
        //PG_CHARGE_SETTLE_AUTO에 있는걸 정산완료 처리한다.
        String day = stlDay.substring(0,4) + "년 " + stlDay.substring(4,6) + "월 " + stlDay.substring(6) + "일";

        VactAutoChargeSettleDAO dao = new VactAutoChargeSettleDAO();
        List<SharedMap<String, Object>> getAutoChargePayOutList = dao.getAutoChargePayOutList(stlDay);

        if(getAutoChargePayOutList.size() > 0) {
            msgBody = "[" + day + "] 자동충전정산 정산금 지급 시작";
            logger.info(msgBody);

            for(SharedMap<String, Object> data : getAutoChargePayOutList) {

                if(data.getLong("stlAmount") > 0) {
                    //가맹점 잔액 더해주기
                    String mchtId = data.getString("mchtId");

                    SharedMap<String, Object> mchtBalance = dao.getMchtBalance(mchtId);
                    long balance = mchtBalance.getLong("balance") + data.getLong("stlAmount");

                    //PG_CHARGE_SETTLE 데이터 만들기
                    SharedMap<String, Object> trxMap = new SharedMap<>();
                    trxMap.put("trxId", dao.getVactId());
                    trxMap.put("mchtId", mchtId);
                    trxMap.put("trxType", "입금");
                    trxMap.put("trxUnit", "가상계좌정산");

                    String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
                    trxMap.put("trxDay", regDate.substring(0, 8));
                    trxMap.put("trxTime", regDate.substring(8));
                    trxMap.put("amount", data.getLong("payAmt"));
                    trxMap.put("fee", data.getLong("payFee"));
                    trxMap.put("feeVat", data.getLong("payVat"));
                    trxMap.put("bankFee", data.getLong("bankFee"));

                    trxMap.put("netAmount", data.getLong("stlAmount"));
                    trxMap.put("balance", balance);
                    trxMap.put("trackId", data.getString("stlId"));

                    trxMap.put("summary", "가상계좌 자동충전 정산금 지금");
                    trxMap.put("regId", "SYSTEM");
                    trxMap.put("regDay", regDate.substring(0, 8));

                    if(dao.insertChargeSettle(trxMap)) {
                        if(!dao.updateChargeSettleAuto(data.getString("stlId"))) {
                            msgBody = "[자동충전정산] PG_CHARGE_SETTLE_AUTO 지급완료처리 실패. 확인요망 [" + data.getString("stlId") + "]";
                            logger.info(msgBody);
                            dayErr++;
                        }
                    } else {
                        msgBody = "[자동충전정산] PG_CHARGE_SETTLE INSERT 실패. 확인요망 [" + mchtId + "]";
                        logger.info(msgBody);
                        dayErr++;
                    }

                    logger.info("가맹점 ID : [{}]", mchtId);
                    logger.info("정산금액 : [{}]", data.getLong("stlAmount"));
                    logger.info("잔액 : [{}]", balance);
                }
            }

            msgBody = "[" + day + "] 자동충전정산 정산금 지급종료";
            logger.info(msgBody);

        }
    }



}
