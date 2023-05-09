package com.pgmate.dm.main;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.dm.dao.VactAccountDAO;
import com.pgmate.dm.dao.VactAuthDAO;
import com.pgmate.dm.util.FirmClient;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.conf.Firm;
import com.pgmate.lib.conf.FirmLoader;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;

/**
 * CreateBy PYS
 * 입금제한횟수 넘은 가상계좌를 해지
 * 조건에 맞는 가상계좌를 해지 API로 보내버리기
 */
public class VactAccountTerminate {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SmsGw smsGw = null;
    private String msgBody = "";

    public static void main(String[] args) { new VactAccountTerminate(); }

    public VactAccountTerminate() {
        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
        smsGw = new SmsGw();

        logger.info("==============================");
        logger.info("VactAccountTerminate Start");

        //매일 23:30~00:30분까지는 은행 점검시간이라서 기능막음
        if(currentTime > 233000 || currentTime < 3000) {
            logger.info("- -- --- ---- ---- ---- 은행점검 시간입니다. ---- ---- ---- --- -- -");
        } else {
            searchVactAccount();
        }

        logger.info("VactAccountTerminate End");
        logger.info("==============================");
    }

    public void searchVactAccount() {

        VactAuthDAO dao = new VactAuthDAO();
        List<SharedMap<String, Object>> accountList = dao.getTerminateAccount(100);

        if(accountList.size() > 0) {
            logger.info("=================================================");
            logger.info("입금횟수초과 가상계좌 만료 처리 시작");
            logger.info("=================================================");

            for(SharedMap<String, Object> data : accountList) {
                logger.info("=================================================");
                logger.info("만료예정 가상계좌 : {}", data.getString("account"));
                logger.info("=================================================");

                String vactBankCd = data.getString("bankCd");
                String companyCd = data.getString("companyCd");
                String vitualAccount = data.getString("account");
                String bankCd = data.getString("withdrawBankCd");
                String account = dao.getAESDec(data.getString("withdrawAccount"));
                String holderName = data.getString("holderName");
                String regType = data.getString("regType");
                String identity = dao.getAESDec(data.getString("identity"));

                logger.info("=================================================");
                logger.info("입금횟수초과 가상계좌 조회, 펌뱅킹 전송 : {} {} {} {}", vitualAccount, bankCd, account, holderName);
                logger.info("=================================================");


                FirmBean firmBean = connectFirm(vactBankCd, companyCd, vitualAccount, bankCd, account, holderName, regType, identity);


                if(firmBean.resultCd.equals("0000")) {
                    //HT_VACT_REG에 INSERT
                    dao.insertHtVactReg(data.getString("mchtId"), vactBankCd, vitualAccount,"2", regType, identity,
                            bankCd,account,holderName, "", "", "", firmBean.resultCd, firmBean.resultMsg);

                    //221222_PYS : 가상계좌 해지 로직변경

                    //PYS : 상태 바꾸기전에 입금횟수초과 상태 DB에 남기기
                    dao.insertHtVactDtl(data.getString("issueId"), firmBean.resultCd, firmBean.resultMsg);

                    //PYS : 입금횟수초기화시 예금주명 초기화
                    String mchtName = dao.getMchtMngVactByMchtId(data.getString("mchtId")).getString("holderName");
                    //vact_dtl 상태='대기', 나머지 기본값으로 변경
                    if(dao.updateVactDtlReady(data.getString("issueId"), mchtName)) {
                        logger.info("=================================================");
                        logger.info("입금횟수초과 가상계좌 대기상태로 변경: {} ", data.getString("issueId"));
                        logger.info("=================================================");

                        //HT_VACT_DTL에 INSERT
                        dao.insertHtVactDtl(data.getString("issueId"), firmBean.resultCd, firmBean.resultMsg);

                        //230504_PYS : 해지된거 노티전송
                        String hookAddr = dao.getMchtMngVactByMchtId(data.getString("mchtId")).getString("statusHookAddr");
                        SharedMap<String, Object> statusMap = new SharedMap<>();
                        statusMap.put("trxId", VactAccountDAO.getNotiId());
                        statusMap.put("mchtId", data.getString("mchtId"));
                        statusMap.put("vactAccount", data.getString("account"));
                        statusMap.put("vactStatus", "대기");
                        statusMap.put("holderName", dao.getMchtMngVactByMchtId(data.getString("mchtId")).getString("holderName"));

                        new VactAccountStatusHook(hookAddr, statusMap, "0").start();

                    } else {
                        logger.info("PG_VACT_DTL 상태 변경 실패 : {}", data.getString("issueId"));
                        msgBody = "PG_VACT_DTL 상태 변경 실패 : [ " + data.getString("issueId") + " ]";
                    }

                    //PYS : PG_VACT_REG테이블 삭제처리
                    if(dao.deleteVactReg(vitualAccount, bankCd, account, holderName)) {
                        logger.info("=================================================");
                        logger.info("PG_VACT_REG 삭제완료 : {} {} ", data.getString("issueId"), vitualAccount);
                        logger.info("=================================================");
                    } else {
                        logger.info("PG_VACT_REG 삭제 실패 : {} {}", data.getString("issueId"), vitualAccount);
                        msgBody = "PG_VACT_REG 삭제 실패 : [ " + data.getString("issueId") + " ]";
                    }
                } else {
                    logger.info("=================================================");
                    logger.info("입금횟수초과 가상계좌 조회, 펌뱅킹 실패 : {} {} {} {}", vitualAccount, bankCd, account, holderName);
                    logger.info("=================================================");
                    msgBody = "입금횟수초과 가상계좌 조회, 펌뱅킹 실패 : [ " + vitualAccount + " ]";
                }
            }
        }

        if(!msgBody.equals("")) {
            smsGw.sendMessage("0", "4", msgBody);
        }

    }

    public FirmBean connectFirm(String vactBankCd,String companyCd, String vitualAccount, String bankCd, String account, String holderName, String regType, String identity) {
        String firmServer = "10.100.200.10";
        int firmPort = 10006;
        int firmTimeOut = 60000;

        FirmBean firmBean = new FirmClient(firmServer, firmPort, firmTimeOut).vactUnReg(vactBankCd, companyCd, vitualAccount, bankCd, account, holderName, regType, identity);

        logger.info("입금횟수 초과 펌뱅킹 결과 : {} {} ", firmBean.resultCd, firmBean.resultMsg);

        return firmBean;
    }


}
