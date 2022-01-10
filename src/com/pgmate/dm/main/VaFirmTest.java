package com.pgmate.dm.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.util.KSBankSeed;
import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class VaFirmTest {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VaFirmTest.class );
	private String FCS_SVC_CODE	= "5000";
	private String SVC_CODE	= "1000";
	private String serverAuthKey=  "12345678abcdefgh12345678";
	private String sendAuthKey =  "XHuyz1RWwXKZ0qpS";
    
	public static void main(String[] args){
		new VaFirmTest();
	}
	
	public VaFirmTest() {
		vactNewIn();
	}
	
	public void vactNewIn(){
		long time 					= System.currentTimeMillis();
		logger.info("=================================== FIRM TEST START ======================================");
		String reqMsg  				= newSpec();
		byte[] request				= null;
		byte[] response 			= null;
		String message				= "";
    	Socket socket 				= null;
    	OutputStream output 		= null;
    	InputStream input			= null;
    	byte[] key					= null;
    	
     	try{
    		socket = new Socket();
    		message= "소켓 오픈 오류";
    		
    		socket.connect(new InetSocketAddress("121.138.30.10", 19237));
    		socket.setSoTimeout(60000);
    		
    		output = socket.getOutputStream();
    		message= "데이터 전송 실패";
    		key = generateKey();
    		request = encrypt(key,reqMsg);
    		logger.info("-> KSNET reqMsg [{}]",reqMsg);

    		output.write(request);
    		output.flush();
    		
    		message= "데이터 수신 오류";
    		input = socket.getInputStream();
    		
    		ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int bcount = 0;
            byte[] buf = new byte[8192];
            int read_retry_count = 0;
            while(true) {
    			int n = input.read(buf);
                if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
                else if (n == -1) break;
                else  { // n == 0
                    if (++read_retry_count >= 5)
                      throw new IOException("inputstream-read-retry-count(5) exceed !");
                }
                if(input.available() == 0){ break; }
            }
            bout.flush();
            response = bout.toByteArray();
            bout.close();
    		
            if(response != null){
            	byte[] resBuf = new byte[response.length-4];
            	System.arraycopy(response, 4, resBuf, 0, response.length-4);
            	response  = decrypt(key, resBuf);
            }else{
            	logger.info("<- KSNET [null]");
            }
		}catch(Exception e){
			logger.info("KSNET COMM ERROR [{}], [{}]", e.getMessage(), e.getStackTrace());
			
			logger.info("COMM MESSAGE : {} ",message);
		}finally{
			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){socket.close();}
			}catch(IOException io){
			}
			
			logger.info("<- KSNET [{}],{},{}",CommonUtil.toString(response),response.length,(System.currentTimeMillis()-time));
		}
     	logger.info("=================================== FIRM TEST END ======================================");
	}
	
	public String newSpec() {
		String account = "110269862726";
		String amount = "1004";
		String bankCode = "088";
		String mAccount = "1005004107798";
		
		String head = "         00013614  01001001047412" + CommonUtil.getCurrentDate("yyyyMMddHHmmss") + "                000000               020             ";
		
		logger.info("newSpec head : [{}]", head);
		
		String sige = sign(account,amount, bankCode, mAccount);
		logger.info("newSpec head : [{}]", sige);
		
		String body = "1005004107798          " + sige + "0000000001004 0000000000000  110269862726   000000000                                                                             088                                      ";
		logger.info("newSpec head : [{}]", body);
		
		String spec = head + body;
		
		return spec;
	}
	
	private String sign(String account, String amount, String bankCode, String mAccount) {
		String sign ="";

		String transactionDate 	= CommonUtil.getCurrentDate("yyMMdd");
		
		String signTemp = transactionDate + rightZeroFill(account,15) + CommonUtil.zerofill(amount,13) 
	       + CommonUtil.zerofill(bankCode,3) + rightZeroFill(mAccount,15);
		
		int fSign = 0;
		for(int i=0 ; i< signTemp.length();i++){
			fSign += Character.getNumericValue(signTemp.charAt(i));
		}
		
		sign = CommonUtil.zerofill(fSign,3)+CommonUtil.zerofill(CommonUtil.parseInt(amount) % fSign,3);

		return sign;	
	}
	
	public static String rightZeroFill(String str,int len){
		int strLen = str.length();
		
		if(len < strLen){
			return str.substring(0,len);
		}else if(len == strLen){
			return str;
		}else{
			String temp = "";
			for(int i=0;i< (len-strLen) ; i++){
				temp += "0";
			}
			return str+temp;
		}
	}
	
	private byte[] generateKey(){
		return new StringBuffer().append(System.currentTimeMillis()).append(Long.MAX_VALUE).substring(0,16).getBytes();
	}
	
	private byte[] encrypt(byte[] key , String req){
		byte[] buf = null;
		if(req.substring(9,12).equals("FCS")){
			logger.info("ENCRYPT : {}", FCS_SVC_CODE);
			buf = (FCS_SVC_CODE+sendAuthKey+req).getBytes();
		}else{
			logger.info("ENCRYPT : {}",SVC_CODE);
			buf = (SVC_CODE+sendAuthKey+req).getBytes();
		}
		return kscms_encrypt(key,buf);
		
	}
	
	private  byte[] decrypt(byte[] key, byte[] buf){
		try{
			return ks_seed_decrypt(key,buf);
		}catch(Exception e){
			logger.error("kscms_decrypt error : {}",e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] ks_seed_encrypt(byte[] kbuf, byte[] mbuf) throws java.security.NoSuchProviderException,javax.crypto.BadPaddingException,javax.crypto.NoSuchPaddingException,java.security.InvalidAlgorithmParameterException,java.security.NoSuchAlgorithmException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException{
		byte tdata[]	= new  KSBankSeed(kbuf).cbc_encrypt(mbuf) ;
		return tdata;
	}
	
	private byte[] ks_seed_decrypt(byte[] kbuf, byte[] mbuf) throws java.security.NoSuchProviderException,javax.crypto.NoSuchPaddingException,java.security.InvalidAlgorithmParameterException,javax.crypto.BadPaddingException,java.security.NoSuchAlgorithmException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException{
		byte tdata[]	= new  KSBankSeed(kbuf).cbc_decrypt(mbuf) ;
		return tdata;
	}
	
	private byte[] kscms_encrypt(byte[] p_kbuf, byte[] p_dbuf){
		try{
			byte[] e_kbuf	= ks_rsa_encrypt(p_kbuf);

			byte[] e_dbuf	= ks_seed_encrypt(p_kbuf, p_dbuf);
			byte[] e_sbuf	= new byte[4+1+e_kbuf.length+4+e_dbuf.length];

			int midx = 0;
			System.arraycopy("01292".getBytes(), 0	,e_sbuf	,midx,  5); midx+= 5;
			System.arraycopy(e_kbuf, 0 , e_sbuf,midx, e_kbuf.length); midx+=e_kbuf.length	;

			System.arraycopy(("0000".substring(String.valueOf(e_dbuf.length).length(),4) + e_dbuf.length).getBytes()	,0	,e_sbuf	,midx,  4			); midx+= 4				;
			System.arraycopy(e_dbuf, 0, e_sbuf,midx, e_dbuf.length); midx+=e_dbuf.length	;

			return e_sbuf;
		}catch(Exception e){
			logger.error("kscms_encrypt error : {}",e.getMessage());
		}

		return null;
	}
	
	private byte[] ks_rsa_encrypt(byte[] sbuf) throws java.security.NoSuchProviderException,javax.crypto.NoSuchPaddingException,javax.crypto.BadPaddingException,java.security.NoSuchAlgorithmException,java.security.spec.InvalidKeySpecException,java.security.InvalidKeyException,javax.crypto.IllegalBlockSizeException{
		BigInteger modulus			= new BigInteger("d4846c2b8228dddfab9e614da2a324c1cc7b29d848cc005624d3a09667a2aab9073290bace6aa536ddceb3c47ddda78d9954da06c83aa65b939c5ec773a3787e71bec5a1c077bb446c06b393d2537967645d386b4b0b4ec21372fdc728c56693028c1c3915c1c4279793eb3dccefd6bf49b86cc7d88a47b0d44aba9e73750fcd",16);
		BigInteger publicExponent	= new BigInteger("0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001",16);

		RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, publicExponent);

		KeyFactory keyfactory = KeyFactory.getInstance("RSA");
		java.security.PublicKey publickey = keyfactory.generatePublic(pubKeySpec);

		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

		cipher.init(Cipher.ENCRYPT_MODE, publickey);

		byte[] rbuf = cipher.doFinal(sbuf);

		return rbuf;
	}
}
