package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

public class EformStatusDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.EformStatusDAO.class );
	public EformStatusDAO() {
		super.setDebug(false);
	}

	/**
	 * 전자계약서 진행 상태 조회 대상 SELECT
	 */
	public List<SharedMap<String,Object>> getReceiverMetaId(){
		String q = "SELECT * FROM PG_EFORM WHERE doc_status = 'WR' OR doc_status = 'WSI' OR doc_status = 'WS';";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	/*
	 * 전자계약서 진행 상태 UPDATE
	 */
	public void update(String doc_status, String doc_date, String receiver_meta_id){
		String q = "UPDATE PG_EFORM "
				+"	SET doc_status = '" + doc_status + "', doc_date = '" + doc_date + "' WHERE receiver_meta_id = '" + receiver_meta_id + "'";
		
		logger.info("UPDATE doc_status : {}",super.update(q));
		super.initRecord();
	}

	public void update(String doc_status, String receiver_meta_id){
		String q = "UPDATE PG_EFORM "
				+"	SET doc_status = '" + doc_status + "' WHERE receiver_meta_id = '" + receiver_meta_id + "'";
		
		logger.info("UPDATE doc_status : {}",super.update(q));
		super.initRecord();
	}
}
