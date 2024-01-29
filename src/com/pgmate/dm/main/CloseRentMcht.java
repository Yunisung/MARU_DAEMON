package com.pgmate.dm.main;

import com.pgmate.dm.dao.CloseRentMchtDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CloseRentMcht {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.CloseRentMcht.class );
    private SmsGw smsGw = null;
    private String msgBody = "";

    // 월세앱 가맹점 종료일 처리 (중지 상태로 변경)
    public CloseRentMcht(String cmd) {
        logger.info("===== CLOSE RENT MCHT START =====");

        CloseRentMchtDAO dao = new CloseRentMchtDAO();
        // 종료 처리 될 가맹점 리스트
        List<SharedMap<String,Object>> mchtList = dao.getCloseMchtList(cmd);

        if(mchtList.size() > 0) {
            // 가맹점 종료 처리
            dao.closeMcht(mchtList);
        }

        logger.info("===== CLOSE RENT MCHT END =====");
    }

    public static void main(String[] args){
        new CloseRentMcht(CommonUtil.getCurrentDate("yyyyMMdd"));
    }
}
