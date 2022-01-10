package com.pgmate.dm.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class FirmBean {

	
	public String msgType 	= ""; 	//08001000
	public String userId	= "";	//사용자 ID
	public String userIp	= "";	//사용자 IP
	public String bankCd	= "";	//은행코드
	public String mAccnt	= "";	//계좌번호
	public String resultCd  = "";	//결과코드
	public String resultMsg = "";	//결과메세지
	public long idx			= 0;	//관련 테이블 IDX
	public SharedMap<String,Object> data = new SharedMap<String,Object>();
	
	public FirmBean() {
		// TODO Auto-generated constructor stub
	}
}
