package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.TotalDAO;
import com.pgmate.lib.util.map.SharedMap;

public class DistinctList {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.DistinctList.class);
	
	public static void main(String[] args) throws Exception {
		new DistinctList();
	}

	public DistinctList() throws Exception {
		logger.info("==================================================");
		logger.info("DistinctList START");

		SetDistinctList();

		logger.info("==================================================");
		logger.info("DistinctList END");
	}
	
	public void SetDistinctList() {
		logger.info("==================================================");
		logger.info("SetDistinctList START");
		
		try {
			TotalDAO dao = new TotalDAO();
			SharedMap<String, Object> map = new SharedMap<String, Object>();

			dao.deleteBefore();
			
			String getTel1 = "";
			String getTel2 = "";
			
			//대행사/에이전시/지사/가맹점 email, tel1, tel2 DISTINCT 처리 후 PG_NOTICE_LIST 저장
			List<SharedMap<String, Object>> checkDist = dao.checkDist();
			List<SharedMap<String, Object>> checkAgency = dao.checkAgency();
			List<SharedMap<String, Object>> checkSales = dao.checkSales();
			List<SharedMap<String, Object>> checkMcht = dao.checkMcht();
			
			for(SharedMap<String, Object> getDist : checkDist) {
				map.put("id", getDist.getString("id"));
				map.put("name", getDist.getString("name"));
				map.put("grade", "대행사");
				map.put("email", getDist.getString("email"));
				
				getTel1 = getDist.getString("tel1").replaceAll("-", "");
				getTel2 = getDist.getString("tel2").replaceAll("-", "");
				
				map.put("tel1", getTel1);
				map.put("tel2", getTel2);
				
				dao.insertDistinct(map);
			}
			for(SharedMap<String, Object> getAgency : checkAgency) {
				map.put("id", getAgency.getString("id"));
				map.put("name", getAgency.getString("name"));
				map.put("grade", "에이전시");
				map.put("email", getAgency.getString("email"));
				
				getTel1 = getAgency.getString("tel1").replaceAll("-", "");
				getTel2 = getAgency.getString("tel2").replaceAll("-", "");
				
				map.put("tel1", getTel1);
				map.put("tel2", getTel2);
				
				dao.insertDistinct(map);
			}
			for(SharedMap<String, Object> getSales : checkSales) {
				map.put("id", getSales.getString("id"));
				map.put("name", getSales.getString("name"));
				map.put("grade", "지사");
				map.put("email", getSales.getString("email"));
				
				getTel1 = getSales.getString("tel1").replaceAll("-", "");
				getTel2 = getSales.getString("tel2").replaceAll("-", "");
				
				map.put("tel1", getTel1);
				map.put("tel2", getTel2);
				
				dao.insertDistinct(map);
			}
			for(SharedMap<String, Object> getMcht : checkMcht) {
				map.put("id", getMcht.getString("id"));
				map.put("name", getMcht.getString("name"));
				map.put("grade", "가맹점");
				map.put("email", getMcht.getString("email"));
				
				getTel1 = getMcht.getString("tel1").replaceAll("-", "");
				getTel2 = getMcht.getString("tel2").replaceAll("-", "");
				
				map.put("tel1", getTel1);
				map.put("tel2", getTel2);
				
				dao.insertDistinct(map);
			}
			dao.checkDistinct();
			dao.checkTel();
			
			logger.info("==================================================");
			logger.info("SetDistinctList END");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("계정 정보 중복 체크 업데이트 중 오류발생. 확인요망 [" + e.getMessage() + "]");
		}
	}
}
