package com.pgmate.dm.main;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChargeSettleReturn extends DAO {

    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettleReturn.class );

    private String firmServer = "10.100.200.10";
    private String compNm = "";
    private int frimPort = 10006;

    public static void main(String[] args) { new ChargeSettleReturn();}

    /**
     * 출금 성공한 건이 출금 실패로 처리됐을때
     * 충전정산 거래내역 생성 및 노티 보내는 데몬
     */
    public ChargeSettleReturn() {
        logger.info("==================================================");
        logger.info("ChargeSettleReturn Strart");
        returnProcess();
        logger.info("==================================================");
        logger.info("ChargeSettleReturn End");
    }

    public void returnProcess() {
        List<SharedMap<String, Object>> firmList = getChargeSettleFirmList();
        logger.info("chargeSettle firmList COUNT : {}", firmList.size());

        if(firmList.size() > 0) {
            for(SharedMap<String, Object> data : firmList) {
//                data.put("trxType", "출금");
//                data.put("trxUnit", "펌뱅킹");

                //CHARGE_SETTLE에 추가
                //insertChargeSettle(data);

                //출금성공 로직
                String status = "완료";
                //String idx = getIdx(data.getString("trxId"));

                SharedMap<String, Object> chargeMngMap = getMchtChargeMng(data.getString("mchtId"));

                if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
                    String payLoad = setPayLoad(data, "출금완료", "0000", "정상처리");
                    data.put("payLoad", payLoad);
                    data.put("trxType", "출금");
                    new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
                }

//                updateRefIdUpdate(data.getString("trxId"), idx);
//                updateRefIdUpdate2(data.getString("trxId"), idx);

            }
        }


    }

    public List<SharedMap<String,Object>> getChargeSettleFirmList(){
        String q = "SELECT A.*, FN_AES_DEC(A.account) as decAccount, B.vactBankCd "
                +"	FROM PG_CHARGE_SETTLE_FIRM A INNER JOIN PG_MCHT_MNG_VACT B"
                +"  ON A.mchtId = B.mchtId"
                +"  WHERE A.trxId = 'CS240905619953' "
                +"  order by A.regDate";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public boolean insertChargeSettle(SharedMap<String, Object> trxMap) {
        super.setTable("PG_CHARGE_SETTLE");
        super.setRecord("trxId", trxMap.getString("trxId"));
        super.setRecord("mchtId", trxMap.getString("mchtId"));
        super.setRecord("trxType", trxMap.getString("trxType"));
        super.setRecord("trxUnit", trxMap.getString("trxUnit"));
        super.setRecord("trxDay", trxMap.getString("trxDay"));
        super.setRecord("trxTime", trxMap.getString("trxTime"));
        super.setRecord("amount", trxMap.getLong("amount"));
        super.setRecord("fee", trxMap.getLong("fee"));
        super.setRecord("feeVat", trxMap.getLong("feeVat"));
        super.setRecord("bankFee", trxMap.getLong("bankFee"));
        super.setRecord("netAmount", trxMap.getLong("netAmount"));
        super.setRecord("balance", trxMap.getLong("balance"));
        super.setRecord("trackId", trxMap.getString("trackId"));
        super.setRecord("refId", trxMap.getString("refId"));
        super.setRecord("bankCd", trxMap.getString("bankCd"));
        super.setRecord("bankName", trxMap.getString("bankName"));
        super.setRecord("account", trxMap.getString("account"));
        super.setRecord("holder", trxMap.getString("holder"));
        super.setRecord("recordInfo", trxMap.getString("recordInfo"));
        super.setRecord("summary", trxMap.getString("summary"));
        super.setRecord("regId", trxMap.getString("regId"));
        super.setRecord("regDay", trxMap.getString("regDay"));

        boolean result = super.insert();
        super.initRecord();
        logger.info("insert PG_CHARGE_SETTLE [{}]", result);
        return result;
    }

    public String getIdx(String trxId){
        super.setTable("PG_FIRM_TRX");
        super.setColumns("idx");
        super.addWhere("filler",trxId,eq);
        super.setLimit(1);

        RecordSet rset = super.search();
        super.initRecord();

        return rset.getRowFirst().getString("idx");
    }

    public SharedMap<String, Object> getMchtChargeMng(String mchtId) {
        super.setTable("PG_MCHT_CHARGE_MNG");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId,eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public String setPayLoad(SharedMap<String, Object> sharedMap, String status, String resultCd, String resultMsg){
        SharedMap<String, String> payLoadMap = new SharedMap<String, String>();
        payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
        payLoadMap.put("trxId",sharedMap.getString("trxId"));
        payLoadMap.put("trxDay",sharedMap.getString("trxDay"));
        payLoadMap.put("trxTime",sharedMap.getString("trxTime"));
        payLoadMap.put("status",status);
        payLoadMap.put("trackId",sharedMap.getString("trackId"));
        payLoadMap.put("resultCd",resultCd);
        payLoadMap.put("resultMsg",resultMsg);
        payLoadMap.put("amount",sharedMap.getString("amount"));
        String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
        return payLoad;
    }

    public boolean updateRefIdUpdate(String trxId, String refId){
        String q = "UPDATE PG_CHARGE_SETTLE "
                + "    SET refId = '"+refId+"'"
                + "	 WHERE trxId = '" + trxId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateRefIdUpdate2(String trxId, String refId){
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM "
                + "    SET refId = '"+refId+"'"
                + "	 WHERE trxId = '" + trxId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }
}
