package com.pgmate.dm.bean;

public class VactHookBean {

	public String vactId 	= "";		//가상계좌거래번호
	public int retry		= 0;		//거래전송재전송횟수
	
	public String mchtId	= "";		//가맹점아이디
	public String issueId	= "";		//가상계좌발급번호
	public String bankCd	= "";		//은행코드
	public String account	= "";		//가상계좌번호
	public String sender	= "";		//입금자명 
	public long amount		= 0;		//금액
	public String trxType	= "";		//입금:20/취소:51구분
	public String rootVactId= "";		//입금취소시 원거래발급번호
	public String trxDay	= "";		//입금일자
	public String trxTime	= "";		//입금시간
	public String trackId	= "";		//가맹점 주문번호
	public String udf1		= "";		//가맹점 영역1
	public String udf2		= "";		//가맹점 영역2
	
	public String stlDay	= "";		//정산예정일자
	public long stlAmount	= 0;		//정산예정금액
	public long stlFee		= 0;		//수수료
	public long stlFeeVat	= 0;		//수수료부가세
	
	public VactHookBean(){
		
	}
}
