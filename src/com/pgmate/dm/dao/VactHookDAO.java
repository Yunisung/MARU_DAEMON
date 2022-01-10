package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

public class VactHookDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	public VactHookDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String, Object>> getRetryList(){
		super.setTable("PG_VACT_TRX");
		super.setColumns("*");
		super.addWhere("hookStatus = '전송장애'");
//		super.addWhere("regDay > date_format(DATE_ADD(NOW(), interval - 3 DAY),'%Y%m%d')");
		super.addWhere("hookAddr != '' ");
		super.addWhere("hookRetry < 10 ");
		super.setOrderBy("regDate asc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}
	
	
	public boolean updateVactTrx(String vactId , String hookStatus,String hookResponse, int hookRetry){
		super.setTable("PG_VACT_TRX");
		String query = "UPDATE PG_VACT_TRX set hookStatus = '"+hookStatus+"' , hookResponse = '"+hookResponse+"' , hookSentDate = now(), hookRetry = "+hookRetry+" WHERE vactId = '"+vactId+"'";
		boolean result = super.update(query);
		super.initRecord();
		return result;
	}
	
}
