package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class BankAccountSearchDAO extends DAO{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.BankAccountSearchDAO.class );

	public BankAccountSearchDAO() {
	}
	
	 public int insertBankData(List<SharedMap<String, Object>> bankDataList) {
		int inserted = 0;
		logger.info("insert PG_BANK_DATA batch : {}", bankDataList.size());
		String query = "INSERT INTO PG_BANK_DATA (sendDate, custName, custBirthDay, custMobileNo, custAccntNo) VALUES (?,?,?,?,?);";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	        Calendar c1 = Calendar.getInstance();
	        String day = sdf.format(c1.getTime()); 
	         
			for (SharedMap<String, Object> map : bankDataList) {
				int i = 1;
				pstmt.setString(i++, day);
				pstmt.setString(i++, map.getString("name"));
				pstmt.setString(i++, map.getString("birthDay"));
				pstmt.setString(i++, map.getString("mobileNo"));
				pstmt.setString(i++, map.getString("accntNo"));
			
				pstmt.addBatch();
				
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			
			inserted += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.info("INSERT batch PG_BANK_DATA error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return inserted;
	}
}


