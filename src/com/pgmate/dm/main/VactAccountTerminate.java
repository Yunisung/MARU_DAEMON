package com.pgmate.dm.main;

import com.pgmate.dm.bean.FirmBean;
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
        List<SharedMap<String, Object>> accountList = dao.getTerminateAccount();

        if(accountList.size() > 0) {
            logger.info("=================================================");
            logger.info("입금횟수초과 가상계좌 만료 처리 시작");
            logger.info("=================================================");

            for(SharedMap<String, Object> data : accountList) {
                SharedMap<String, Object> regData = dao.getVactReg(data.getString("account"));

                String vitualAccount = regData.getString("account");
                String bankCd = regData.getString("withdrawBankCd");
                String account = dao.getAESDec(regData.getString("withdrawAccount"));
                String holderName = regData.getString("holderName");

                logger.info("=================================================");
                logger.info("입금횟수초과 가상계좌 조회, 펌뱅킹 전송 : {} {} {} {}", vitualAccount, bankCd, account, holderName);
                logger.info("=================================================");

                FirmBean firmBean = connectFirm(vitualAccount, bankCd, account, holderName);

                //HT_VACT_REG에 INSERT
                dao.insertHtVactReg(
                        regData.getString("mchtId"),
                        regData.getString("bankCd"),
                        regData.getString("account"),
                        "2",
                        bankCd,
                        account,
                        holderName, "", "", "", firmBean.resultCd, firmBean.resultMsg);

                //HT_VACT_DTL에 INSERT
                dao.insertHtVactDtl(data.getString("issueId"), firmBean.resultCd, firmBean.resultMsg);

                if(firmBean.resultCd.equals("0000")) {
                    //vact_dtl 상태='대기', 나머지 기본값으로 변경
                    if(dao.updateVactDtl(data.getString("issueId"))) {
//                    if(dao.deleteVactDtl(data.getString("issueId"))) {
                        if(dao.deleteVactReg(vitualAccount, bankCd, account, holderName)) {
                            logger.info("=================================================");
                            logger.info("입금횟수초과 가상계좌 프로세스완료: {} ", data.getString("issueId"));
                            logger.info("=================================================");
                        } else {
                            logger.info("PG_VACT_REG 삭제 실패 : {}", data.getString("issueId"));
                            msgBody = "PG_VACT_REG 삭제 실패 : [ " + data.getString("issueId") + " ]";
                        }
                    } else {
                        logger.info("PG_VACT_DTL 상태 변경 실패 : {}", data.getString("issueId"));
                        msgBody = "PG_VACT_DTL 상태 변경 실패 : [ " + data.getString("issueId") + " ]";
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

    public FirmBean connectFirm(String vitualAccount, String bankCd, String account, String holderName) {
        String firmServer = "10.100.200.10";
        int firmPort = 10006;
        int firmTimeOut = 60000;

        FirmBean firmBean = new FirmClient(firmServer, firmPort, firmTimeOut).vactUnReg("089", vitualAccount, bankCd, account, holderName);

        logger.info("입금횟수 초과 펌뱅킹 결과 : {} {} ", firmBean.resultCd, firmBean.resultMsg);

        return firmBean;
    }


}
