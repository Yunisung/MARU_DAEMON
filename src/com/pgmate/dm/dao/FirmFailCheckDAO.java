package com.pgmate.dm.dao;

import java.util.List;

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
}
