package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.main.Cache;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class KsnetDiffDownloadDAO extends DAO{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.KsnetDiffDownloadDAO.class );

	public KsnetDiffDownloadDAO() {
	}
	
	public int updateTrxDiff(List<SharedMap<String, Object>> trxList) {
		int updated = 0;
		logger.info("UPDATE PG_TRX_DIFF batch : {}", trxList.size());
		String query = "UPDATE `PG_TRX_DIFF` SET recordType = ?, resultCd = ?, mchtType = ?, mchtCode = ?, cardType = ?, diffStlAmt = ?, diffStlDay = ?, downDay = ? WHERE trxId = ?;";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;

			for (SharedMap<String, Object> map : trxList) {
				int i = 1;
				pstmt.setString(i++, map.getString("recordType"));
				pstmt.setString(i++, map.getString("resultCd"));
				pstmt.setString(i++, map.getString("mchtType"));
				pstmt.setString(i++, map.getString("mchtCode"));
				pstmt.setString(i++, map.getString("cardType"));
				pstmt.setLong(i++, map.getLong("diffStlAmt"));
				pstmt.setString(i++, map.getString("diffStlDay"));
				pstmt.setString(i++, map.getString("downDay"));
				pstmt.setString(i++, map.getString("trxId"));
				
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					updated += pstmt.executeBatch().length;
				}
			}

			updated += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.error("update batch PG_TRX_DIFF error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return updated;
	}
	
	public int updateTrxCheckDiff(List<SharedMap<String, Object>> trxList) {
		int updated = 0;
		logger.debug("UPDATE PG_TRX_CHECK_DIFF batch : {}", trxList.size());
		String query = "UPDATE `PG_TRX_DIFF` SET recordType = ?, resultCd = ? WHERE trxId = ?;";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;

			for (SharedMap<String, Object> map : trxList) {
				int i = 1;
				pstmt.setString(i++, map.getString("recordType"));
				pstmt.setString(i++, map.getString("resultCd"));
				pstmt.setString(i++, map.getString("trxId"));
				
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					updated += pstmt.executeBatch().length;
				}
			}

			updated += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.debug("update batch PG_TRX_CHECK_DIFF error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return updated;
	}
	
	public int insertMchtDiffDownLoad(List<SharedMap<String, Object>> trxList) {
		int inserted = 0;
		logger.info("INSERT PG_MCHT_DIFF_DOWNLOAD batch : {}", trxList.size());
		String query = "INSERT INTO `PG_MCHT_DIFF_DOWNLOAD` (`mchtId`, `recordType`, `regType`, `compNo`, `vanId`, `mchtCompNo`, `uploadDay`, `cardReqDay`, `resultDay`, `cardCode`, `cardName`, `intrsFree`, `regResult`, `failMsg`, `filler`, `regDay`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE uploadDay = ?, cardReqDay = ?, resultDay = ?, intrsFree = ?, regResult = ?, failMsg = ?, regDay = ?; "; 
				
		
		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			
			int batchSize = 100;
			int count = 0;
			
			for (SharedMap<String, Object> map : trxList) {
				int i = 1;
				pstmt.setString(i++, map.getString("mchtId"));
				pstmt.setString(i++, map.getString("recordType"));
				pstmt.setString(i++, map.getString("regType"));
				pstmt.setString(i++, map.getString("compNo"));
				pstmt.setString(i++, map.getString("vanId"));
				pstmt.setString(i++, map.getString("mchtCompNo"));
				pstmt.setString(i++, map.getString("uploadDay"));
				pstmt.setString(i++, map.getString("cardReqDay"));
				pstmt.setString(i++, map.getString("resultDay"));
				pstmt.setString(i++, map.getString("cardCode"));
				pstmt.setString(i++, map.getString("cardName"));
				pstmt.setString(i++, map.getString("intrsFree"));
				pstmt.setString(i++, map.getString("regResult"));
				pstmt.setString(i++, map.getString("failMsg"));
				pstmt.setString(i++, map.getString("filler"));
				pstmt.setString(i++, map.getString("regDay"));
				
				pstmt.setString(i++, map.getString("uploadDay"));
				pstmt.setString(i++, map.getString("cardReqDay"));
				pstmt.setString(i++, map.getString("resultDay"));
				pstmt.setString(i++, map.getString("intrsFree"));
				pstmt.setString(i++, map.getString("regResult"));
				pstmt.setString(i++, map.getString("failMsg"));
				pstmt.setString(i++, map.getString("regDay"));
				
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			
			inserted += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.info("INSERT batch PG_MCHT_DIFF_DOWNLOAD error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return inserted;
	}
	
	public List<SharedMap<String, Object>> getErrList() {
		String q = "SELECT A.mchtId, A.amount, A.cardType, B.* "
				+ "FROM PG_TRX_CAP A , PG_TRX_CAP_DTL B "
				+ "WHERE A.trxId IN ("
				+ "SELECT trxId FROM PG_TRX_DIFF WHERE resultCd IN ('12')) "
				+ "AND A.capId = B.capId";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows(); 
	}
	
	public String getMchtId(SharedMap<String, Object> map) {
		super.setTable("PG_MCHT_DIFF_UPLOAD");
		super.setColumns("mchtId");
		super.addWhere("mchtCompNo", map.getString("mchtCompNo"),eq);
		super.addWhere("vanId", map.getString("vanId"),eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("mchtId");
	}

	public List<SharedMap<String,Object>> getDiffList(){
		String q = "SELECT A.*, B.capId "
				+ "FROM PG_TRX_DIFF A "
				+ "LEFT JOIN PG_TRX_CAP B ON A.trxId = B.trxId "
				+ "LEFT JOIN PG_TRX_CAP_DTL C ON B.capId = C.capId "
				+ "WHERE A.recordType = 'R' "
				+ "AND A.resultCd = '00' "
				+ "AND C.stlDiffVanDay IS NULL "
				+ "AND C.capId IS NOT NULL ";
				
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean updateDiffCapture(String nowDate) {
		String query = "UPDATE PG_TRX_CAP_DTL A "
				+ "INNER JOIN PG_TRX_CAP B ON A.capId = B.capId "
				+ "INNER JOIN PG_TRX_DIFF C ON B.trxId = C.trxId "
				+ "SET A.stlDiffStatus = '입금대기', "
				+ "A.stlDiffVanDay = C.diffStlDay, "
				+ "A.stlDiffVanAmt = IF(C.trxType = '0', C.diffStlAmt, C.diffStlAmt * (-1)), "
				+ "A.stlDiffVanType = C.mchtType "
				+ "WHERE C.downDay = '"+nowDate+"' "
				+ "AND C.resultCd = '00' ";
		return this.update(query);
	}

	public int checkNowMcht(String nowDate) {
		String query = "SELECT COUNT(*) AS cnt FROM PG_MCHT_DIFF_DOWNLOAD WHERE regDay = '"+nowDate+"'";
		RecordSet rset = super.query(query);
		super.initRecord();
		return rset.getRowFirst().getInt("cnt");
	}

	public int checkDownSettle(String nowDate) {
		String query = "SELECT COUNT(*) AS cnt FROM PG_TRX_DIFF WHERE downDay = '"+nowDate+"'";
		RecordSet rset = super.query(query);
		super.initRecord();
		return rset.getRowFirst().getInt("cnt");
	}

	public List<SharedMap<String, Object>> getTrxCap(String nowDate) {
		super.setDebug(true);
		String query = "SELECT A.*, B.capId, C.stlFee,C.stlFeeVat,C.stlDistFee,C.stlDistRate,C.stlAgencyFee,C.stlAgencyRate,C.stlSalesRate,C.stlSalesFee,C.stlVanFee,C.stlDiffType,D.codeName FROM PG_TRX_DIFF A "
				+ "INNER JOIN PG_TRX_CAP B ON A.trxId = B.trxId "
				+ "INNER JOIN PG_TRX_CAP_DTL C ON B.capId = C.capId "
				+ "LEFT JOIN PG_CODE D on A.resultCd = D.code and D.alias = 'DIFF' "
				+ "WHERE A.downDay = '"+nowDate+"'";
		RecordSet rset = super.query(query);
		super.initRecord();
		return rset.getRows();
	}
	
	public SharedMap<String, Object> getAgencyMngById(String agencyId) {
		String key = "PG_MAM_AGENCY_MNG_" + agencyId;
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_AGENCY_MNG");
			super.setColumns("*");
			super.addWhere("agencyId", agencyId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0){
				return Cache.map.put(key, rset.getRow(0));
			}else{
				return new SharedMap<String,Object>();
			}
		}
	}
	
	public SharedMap<String, Object> getMchtMngById(String mchtId) {
		String key = "PG_MCHT_MNG_" + mchtId;
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0){
				return Cache.map.put(key, rset.getRow(0));
			}else{
				return new SharedMap<String,Object>();
			}
		}
	}
	
	public SharedMap<String, Object> getSalesMngById(String salesId) {
		String key = "PG_MAM_SALES_MNG_" + salesId;
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_SALES_MNG");
			super.setColumns("*");
			super.addWhere("salesId", salesId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0){
				return Cache.map.put(key, rset.getRow(0));
			}else{
				return new SharedMap<String,Object>();
			}
		}
	}
	
	public SharedMap<String, Object> getOrgFee(String van) {
		if(van == null){
			return new SharedMap<String,Object>();
		}
		String key = "PG_ORG_FEE_" + van;
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
			super.setTable("PG_ORG_FEE");
			super.setColumns("*");
			super.addWhere("van", van, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0){
				return Cache.map.put(key, rset.getRow(0));
			}else{
				return new SharedMap<String,Object>();
			}
		}
	}
	
	public int updateTrxCap(String nowDate) {

		List<SharedMap<String, Object>> list = getTrxCap(nowDate);
		String query = "UPDATE PG_TRX_CAP_DTL SET stlDistFee =?, stlDistRate=?, stlAgencyFee =?, stlAgencyRate =?, stlSalesFee =?,stlSalesRate =?, stlDiffAgencyRate =?, stlDiffAgencyFee =?, stlDiffDistRate =?, stlDiffDistFee =?, stlDiffSalesRate =?, stlDiffSalesFee =?, stlDiffVanAmt =?,"
				+ " stlDiffStatus = ?, stlDiffVanType= ?, stlDiffVanCardType= ?, stlDiffVanDay =?, stlDiffResultMsg = ?, benefit = ? WHERE capId = ?;"; 
				
		int inserted = 0;
		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			
			int batchSize = 100;
			int count = 0;
			
			
			for(SharedMap<String, Object> map:list) {
				SharedMap<String, Object> capDtlMap = new SharedMap<String, Object>();
				capDtlMap.put("capId",map.getString("capId"));
				capDtlMap.put("stlDiffResultMsg",map.getString("codeName"));
				String stlDiffVanCardType = "";
				
				if(map.getString("resultCd").equals("00")) {
					SharedMap<String, Object> mchtMngMap = getMchtMngById(map.getString("mchtId"));
					SharedMap<String,Object> orgFeeMap = getOrgFee(map.getString("van"));
					
					//영업라인 차액정산 수수료율 세팅
					double stlDiffAgencyRate = 0;
					double stlDiffDistRate = 0;
					double stlDiffSalesRate = 0;
					double diffRate = 0;

					if(map.getString("cardType").equals("1")) {
						switch(map.getString("mchtType")) {
							case "영세":stlDiffAgencyRate = mchtMngMap.getDouble("diff0CheckAgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff0CheckDistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff0CheckSalesRate");break;
							case "중소1":stlDiffAgencyRate = mchtMngMap.getDouble("diff1CheckAgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff1CheckDistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff1CheckSalesRate");break;
							case "중소2":stlDiffAgencyRate = mchtMngMap.getDouble("diff2CheckAgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff2CheckDistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff2CheckSalesRate");break;
							case "중소3":stlDiffAgencyRate = mchtMngMap.getDouble("diff3CheckAgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff3CheckDistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff3CheckSalesRate");break;
							default :stlDiffAgencyRate = 0;stlDiffDistRate = 0;break;
						}
						diffRate = orgFeeMap.getDouble("diff1CheckRate");
						stlDiffVanCardType = "체크";
					}else {
						switch(map.getString("mchtType")) {
							case "영세":stlDiffAgencyRate = mchtMngMap.getDouble("diff0AgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff0DistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff0SalesRate");break;
							case "중소1":stlDiffAgencyRate = mchtMngMap.getDouble("diff1AgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff1DistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff1SalesRate");break;
							case "중소2":stlDiffAgencyRate = mchtMngMap.getDouble("diff2AgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff2DistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff2SalesRate");break;
							case "중소3":stlDiffAgencyRate = mchtMngMap.getDouble("diff3AgencyRate");stlDiffDistRate = mchtMngMap.getDouble("diff3DistRate");stlDiffSalesRate = mchtMngMap.getDouble("diff3SalesRate");break;
							default :stlDiffAgencyRate = 0;stlDiffDistRate = 0;break;
						}
						diffRate = orgFeeMap.getDouble("diff1Rate");
						stlDiffVanCardType = "신용";
					}
					
					//하위사업자 매출액
					long amount = map.getLong("mchtSalesAmt");
					//차액정산금액 ksnet에서 보내줌
					long diffVanAmt = map.getLong("diffStlAmt");
					//취소일때
					if(!map.getString("trxType").equals("0")) {
						amount = -map.getLong("mchtSalesAmt");
						diffVanAmt = -map.getLong("diffStlAmt"); 
					}
					//에이전시 부과세 계산
					long stlDiffAgencyFee = calcFeeVat(amount, stlDiffAgencyRate);
					//대행사 부과세 계산
					long stlDiffDistFee = calcFeeVat(amount, stlDiffDistRate);
					
					capDtlMap.put("stlDiffAgencyRate",stlDiffAgencyRate);
					capDtlMap.put("stlDiffAgencyFee",stlDiffAgencyFee);
					capDtlMap.put("stlDiffDistRate",stlDiffDistRate);
					capDtlMap.put("stlDiffDistFee",stlDiffDistFee);
					capDtlMap.put("stlDiffSalesRate", stlDiffSalesRate);
					capDtlMap.put("stlDiffSalesFee"	, calcFee(capDtlMap.getLong("stlDiffAgencyFee"), capDtlMap.getDouble("stlDiffSalesRate")));
					
					// 에이전시 차액정산 최종 수수료 : 에이전시 차액정산 수수료 - 지사 차액정산 수수료
					capDtlMap.put("stlDiffAgencyFee", capDtlMap.getLong("stlDiffAgencyFee")-capDtlMap.getLong("stlDiffSalesFee"));

					// 본사차액정산금 계산
					capDtlMap.put("stlDiffRate"	, diffRate);
					capDtlMap.put("stlDiffAmt"	, calcFeeVat(map.getLong("amount"),diffRate));

					//차액정산금액
					capDtlMap.put("stlDiffVanAmt"	, diffVanAmt);
					//영중소 타입
					capDtlMap.put("stlDiffVanType", map.getString("mchtType"));
					//신용,체크카드 구분
					capDtlMap.put("stlDiffVanCardType", stlDiffVanCardType);
					capDtlMap.put("stlDiffStatus", "입금대기");
					capDtlMap.put("stlDiffVanDay", map.getString("diffStlDay"));
					
					
					// 일반 수수료 처리
					if(map.getString("stlDiffType").equals("일반")) {
						if(capDtlMap.getString("stlDiffVanType").equals("일반")) {
							// 기존동일 변동없음
							capDtlMap.put("stlDistFee", map.getLong("stlDistFee"));
							capDtlMap.put("stlDistRate", map.getDouble("stlDistRate"));
							capDtlMap.put("stlAgencyFee", map.getLong("stlAgencyFee"));
							capDtlMap.put("stlAgencyRate", map.getDouble("stlAgencyRate"));
							capDtlMap.put("stlSalesFee", map.getLong("stlSalesFee"));
							capDtlMap.put("stlSalesRate", map.getDouble("stlSalesRate"));
						}else {
							// 영중소로 차액정산 반영할 경우
							capDtlMap.put("stlDistFee", 0);
							capDtlMap.put("stlDistRate", 0);
							capDtlMap.put("stlAgencyFee", 0);
							capDtlMap.put("stlAgencyRate", 0);
							capDtlMap.put("stlSalesFee", 0);
							capDtlMap.put("stlSalesRate", 0);
						}
					}else {
						// 영중소가 아닌 일반으로 올경우
						if(capDtlMap.getString("stlDiffVanType").equals("일반")) {
							// 일반 수수료로 계산진행
							capDtlMap.put("stlAgencyRate", mchtMngMap.getDouble("rate")-mchtMngMap.getDouble("agencyRate"));
							capDtlMap.put("stlAgencyFee", calcFeeVat(amount, capDtlMap.getDouble("stlAgencyRate")));
							capDtlMap.put("stlSalesRate", mchtMngMap.getDouble("salesRate"));
							capDtlMap.put("stlSalesFee"	, calcFee(capDtlMap.getLong("stlAgencyFee"), capDtlMap.getDouble("stlSalesRate")));
							capDtlMap.put("stlDistRate"	, mchtMngMap.getDouble("agencyRate")-mchtMngMap.getDouble("distRate"));
							if(capDtlMap.getDouble("stlDistRate") < 0){
								capDtlMap.put("stlDistFee"	, 0);
							}else{
								capDtlMap.put("stlDistFee"	, calcFeeVat(amount, capDtlMap.getDouble("stlDistRate")));
							}
							// 에이전시 최종 수수료 : 에이전시 수수료 - 지사 수수료
							capDtlMap.put("stlAgencyFee", capDtlMap.getLong("stlAgencyFee")-capDtlMap.getLong("stlSalesFee"));
							
						}else {
							// 기존동일 변동없음
							capDtlMap.put("stlDistFee", map.getLong("stlDistFee"));
							capDtlMap.put("stlDistRate", map.getDouble("stlDistRate"));
							capDtlMap.put("stlAgencyFee", map.getLong("stlAgencyFee"));
							capDtlMap.put("stlAgencyRate", map.getDouble("stlAgencyRate"));
							capDtlMap.put("stlSalesFee", map.getLong("stlSalesFee"));
							capDtlMap.put("stlSalesRate", map.getDouble("stlSalesRate"));
						}
					}
					
					//가맹점수수료 + 가맹점수수료부과세 - 대행사 수수료 - 에이전시 수수료 - 지사수수료 - 입금수수료
					long benefit1 = map.getLong("stlFee")+map.getLong("stlFeeVat")-capDtlMap.getLong("stlDistFee")-capDtlMap.getLong("stlAgencyFee")-capDtlMap.getLong("stlSalesFee")-map.getLong("stlVanFee");
					//차액정산입금예정액 - 대행사 부과세 - 에이전시 부과세
					long benefit2 = capDtlMap.getLong("stlDiffVanAmt") - (stlDiffDistFee + stlDiffAgencyFee);
					
					capDtlMap.put("benefit"		, benefit1 + benefit2);
				}else {
					//차액정산에 실패하였으므로 일반 수수료는 기존과 동일
					capDtlMap.put("stlDistFee", map.getLong("stlDistFee"));
					capDtlMap.put("stlDistRate", map.getDouble("stlDistRate"));
					capDtlMap.put("stlAgencyFee", map.getLong("stlAgencyFee"));
					capDtlMap.put("stlAgencyRate", map.getDouble("stlAgencyRate"));
					capDtlMap.put("stlSalesFee", map.getLong("stlSalesFee"));
					capDtlMap.put("stlSalesRate", map.getDouble("stlSalesRate"));
					
					
					// 차액정산 수수료는 0으로 한다
					capDtlMap.put("stlDiffAgencyRate",0);
					capDtlMap.put("stlDiffAgencyFee",0);
					capDtlMap.put("stlDiffDistRate",0);
					capDtlMap.put("stlDiffDistFee",0);
					capDtlMap.put("stlDiffSalesRate",0);
					capDtlMap.put("stlDiffSalesFee",0);
					
					capDtlMap.put("stlDiffVanAmt"	, 0);
					capDtlMap.put("stlDiffVanType", map.getString("mchtType"));
					capDtlMap.put("stlDiffStatus", "차액정산실패");
					capDtlMap.put("benefit"		, map.getLong("stlFee")+map.getLong("stlFeeVat")-capDtlMap.getLong("stlDistFee")-capDtlMap.getLong("stlAgencyFee")-map.getLong("stlVanFee"));
				}
			
				int i = 1;
				
				pstmt.setLong(i++, capDtlMap.getLong("stlDistFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDistRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlAgencyFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlAgencyRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlSalesFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlSalesRate"));
				
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffAgencyRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffAgencyFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffDistRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffDistFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffSalesRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffSalesFee"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffVanAmt"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffStatus"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffVanType"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffVanCardType"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffVanDay"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffResultMsg"));
				pstmt.setLong(i++, capDtlMap.getLong("benefit"));
				pstmt.setString(i++, capDtlMap.getString("capId"));
				
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			
			inserted += pstmt.executeBatch().length;
			conn.commit();
		}catch (Exception e) {
			logger.error("update batch PG_TRX_CAP_DTL error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return inserted;
	}
	
	public int updateErrTrxCap(List<SharedMap<String, Object>> errList, String nowDate) {
		
		String query = "UPDATE PG_TRX_CAP_DTL SET stlDistFee =?, stlDistRate=?, stlAgencyFee =?, stlAgencyRate =?, stlSalesFee =?,stlSalesRate =?,"
				+" stlDiffAgencyRate =?, stlDiffAgencyFee =?, stlDiffDistRate =?, stlDiffDistFee =?, stlDiffSalesRate =?, stlDiffSalesFee =?, stlDiffVanAmt =?,"
				+" stlDiffStatus = ?, stlDiffVanType= ?, stlDiffVanDay =?, stlDiffResultMsg = ?, benefit = ?, stlDiffRate = ?, stlDiffAmt = ? WHERE capId = ?;";
		
		int inserted = 0;
		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			
			int batchSize = 100;
			int count = 0;
		
			for(SharedMap<String, Object> map:errList) {
				SharedMap<String, Object> capDtlMap = new SharedMap<String, Object>();
				SharedMap<String, Object> mchtMngMap = getMchtMngById(map.getString("mchtId"));
				
				long amount = map.getLong("amount");
				
				capDtlMap.put("capId", map.getString("capId"));
				logger.info("err capId : {}", map.getString("capId"));
				
				// 영업라인 정산 금액 설정
				capDtlMap.put("stlAgencyRate", mchtMngMap.getDouble("rate")-mchtMngMap.getDouble("agencyRate"));
				capDtlMap.put("stlAgencyFee", calcFeeVat(amount, capDtlMap.getDouble("stlAgencyRate")));
				capDtlMap.put("stlSalesRate", mchtMngMap.getDouble("salesRate"));
				capDtlMap.put("stlSalesFee"	, calcFee(capDtlMap.getLong("stlAgencyFee"), capDtlMap.getDouble("stlSalesRate")));
				capDtlMap.put("stlDistRate"	, mchtMngMap.getDouble("agencyRate")-mchtMngMap.getDouble("distRate"));
				if(capDtlMap.getDouble("stlDistRate") < 0){
					capDtlMap.put("stlDistFee"	, 0);
				}else{
					capDtlMap.put("stlDistFee"	, calcFeeVat(amount, capDtlMap.getDouble("stlDistRate")));
				}
				// 에이전시 최종 수수료 : 에이전시 수수료 - 지사 수수료
				capDtlMap.put("stlAgencyFee", capDtlMap.getLong("stlAgencyFee")-capDtlMap.getLong("stlSalesFee"));
				
				capDtlMap.put("stlDiffVanDay", nowDate);
					
				// 차액정산 수수료는 0으로 한다
				capDtlMap.put("stlDiffAgencyRate",0);
				capDtlMap.put("stlDiffAgencyFee",0);
				capDtlMap.put("stlDiffDistRate",0);
				capDtlMap.put("stlDiffDistFee",0);
				capDtlMap.put("stlDiffSalesRate",0);
				capDtlMap.put("stlDiffSalesFee",0);
				
				capDtlMap.put("stlDiffVanAmt"	, 0);
				capDtlMap.put("stlDiffVanType", map.getString("mchtType"));
				capDtlMap.put("stlDiffStatus", "입금대기");
				capDtlMap.put("benefit"		, map.getLong("stlFee")+map.getLong("stlFeeVat")-capDtlMap.getLong("stlDistFee")-capDtlMap.getLong("stlAgencyFee")-map.getLong("stlVanFee"));
			
				int i = 1;
				
				pstmt.setLong(i++, capDtlMap.getLong("stlDistFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDistRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlAgencyFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlAgencyRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlSalesFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlSalesRate"));
				
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffAgencyRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffAgencyFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffDistRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffDistFee"));
				pstmt.setDouble(i++, capDtlMap.getDouble("stlDiffSalesRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffSalesFee"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffVanAmt"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffStatus"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffVanType"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffVanDay"));
				pstmt.setString(i++, capDtlMap.getString("stlDiffResultMsg"));
				pstmt.setLong(i++, capDtlMap.getLong("benefit"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffRate"));
				pstmt.setLong(i++, capDtlMap.getLong("stlDiffAmt"));
				pstmt.setString(i++, capDtlMap.getString("capId"));
				
				pstmt.addBatch();
			
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			
			inserted += pstmt.executeBatch().length;
			conn.commit();

		}catch (Exception e) {
			logger.error("update batch PG_TRX_CAP_DTL error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		
		return inserted;
	}
	
	public long calcFee(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		if(amount < 0){
			return -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			return new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
			
		}
	}
	
	public double rateFormat(double rate){
		String pattern = "#.#####";
		DecimalFormat format = new DecimalFormat(pattern);
		return new Double(format.format(rate)).doubleValue();
	}
	
	public long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /100).longValue();
		}else{
			return new Double(amount *10 /100).longValue();
		}
	}
	public long calcFeeVat(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		long fee = 0;
		if(amount < 0){
			fee = -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			fee = new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
		}
		long vat = calcVat(fee);
		return fee+vat;
	}
	
	public String calcDay(String settleType,String today){
		int term = 1;
		if(settleType.startsWith("D")){
			term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
			String day =  getSettleDay(today, term);
			/*
			//오늘 정산 예정일이지만 8시 이후에 요청된 거래는 자동으로 내일로 정산일정이 밀린다.
			if(day.equals(currentDay) && CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) > 8){
				day =  trxDAO.getSettleDay(currentDay,1);
			}*/
			return day;
		}else if(settleType.startsWith("M")){
			term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));
			String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH,1,today).substring(0,6);
			return getSettleDay(nextMonth+CommonUtil.zerofill(term,2));
		}else{
			/*
			term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
			String day =  trxDAO.getSettleDay(today, term);
			/*
			if(day.equals(currentDay) && CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) > 8){
				day =  trxDAO.getSettleDay(currentDay,1);
			}*/
			return "";
		}
	}
	
	public String getSettleDay(String today,int term) {	
		String start = CommonUtil.toString(term-1);
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days > '"+today+"' AND status ='no' limit "+start+",1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
	
	public String getSettleDay(String today) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days >= '"+today+"' AND status ='no' limit 1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
}


