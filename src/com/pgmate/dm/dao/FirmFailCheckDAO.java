package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class FirmFailCheckDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.FirmFailCheckDAO.class );
	
	public FirmFailCheckDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 펌 출금 실패건들중 1시간 이내의 거래건들만 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getFirmFailList(){
		String q = "SELECT * "
				+"	  FROM PG_FIRM_TRX "
				+"   WHERE NOW() > DATE_ADD(regDate, INTERVAL 1 HOUR) and "
				+"   	   resultCd != '0000' "
				+"   order by regDate;";
		
		q = "select * from PG_FIRM_TRX where sendDatee = '20190719';";
				
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public static String getResultMsg(String code){
		if(code.equals("XXXX")){
			return "통신장애";
		}
		String query = " SELECT message FROM PG_FIRM_CODE WHERE `code` = ?";

		DBManager db 			= null;
		PreparedStatement pstmt	= null;
		Connection conn			= null;
		ResultSet rset			= null;
		String result			= "";

		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			pstmt.setString(1,code);
			rset 	= pstmt.executeQuery();

			while(rset.next()){
				result = CommonUtil.nToB(rset.getString("message"));
			}
		}catch(Exception e){
			logger.info("DB Error : {} , {} , [{}]",Thread.currentThread().getStackTrace()[1].getMethodName(),e.getMessage(),query);
		}finally{
			db.close(conn,pstmt,rset);
		}

		return result;
	}
}
