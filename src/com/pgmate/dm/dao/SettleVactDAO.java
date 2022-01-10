package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
abstract class SettleVactDAO extends DAO {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleDAO.class );

	public SettleVactDAO() {
		super.setDebug(false);
	}
	
	public String getFunction(String function,String value){
		String returnVal = "";
		String query = "SELECT "+function+"(?) as val";
		
		DBManager db 			= null;
		PreparedStatement pstmt = null;
		Connection 	conn		= null;
		ResultSet rset			= null;

		try {
			db 			= DBFactory.getInstance();
			conn		= db.getConnection();
			pstmt		= conn.prepareStatement(query);
			pstmt.setString(1,value);
			rset		= pstmt.executeQuery();
			
			while(rset.next()){
				returnVal = rset.getString(1);
			}
			conn.commit();
		}catch(Exception t){
			logger.debug("sql error : {}, query : {}",t.getMessage(),query);
		}finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}
	
	public String getSettleId(){
		return "S"+getFunction("FN_NEXTVAL2","SETTLE");
	}
}
