package com.pgmate.dm.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <ul>
 * <li>(주)KSNET의 PG서비스 시스템에 파일을 전송하거나 수신할 때 사용하는 API입니다.
 * <li>실행 환경은 jre 1.5.x 이상입니다.
 * <li>최대 파일 송수신 크기는 1Gbytes 미만입니다.
 * <li>업로드시 실제 파일 이름은 KSNET 시스템에서 저장하지 않습니다. (KSNET 시스템 내에서 이름이 바뀌어 저장됩니다. )
 * <li>다운로드 대상 파일이 바이너리 파일 이면서 여러 개일 경우 zip으로 압축되어 다운로드 될 수 있습니다. 
 * </ul>
 * <p/>
 * <p/>
 * 
 * @author 전용택(ytjeon@ksnet.co.kr)
 * @version 2.0
 * @since 2017.05.22.
 * @see KSNetPGUploader
 * @see KSNetPGDownloader
 * 
 * 
 * 
 */
public class KSPGFtsUpDownLib
{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.util.KSPGFtsUpDownLib.class );
	// [S] KSNET 정의 영역
	// ////////////////////////////////////////////////////////////
	private static final int LOG_DEFAULT = 0;

	/**
	 * LOG ERROR = 1
	 */
	public static final int LOG_ERROR = 1;

	/**
	 * LOG WARNING = 2
	 */
	public static final int LOG_WARNING = 2;

	/**
	 * LOG INFORMATION = 3
	 */
	public static final int LOG_INFO = 3;

	/**
	 * LOG for Debugging = 4
	 */
	public static final int LOG_DEBUG = 4;

	/*
	 * 암호화 구분 값
	 */
	public static final byte ENCRYPT_TYPE = '4';

	/**
	 * 패킷 사이즈
	 */
	private static final int PACKET_SIZE = 4096;

	// ---[S][업체 개발자 필요시 변경 사항] 파일을 읽어 upload/download 할 경우 문자 인코딩 방식(charset)

	private static int LOG_LEVEL = LOG_ERROR;

	// -- [S] 오류코드 정의 ---------------------------------------------
	/**
	 * 응답코드 : 성공
	 */
	public static final int SUCCESS = 0;
	
	/**
	 * 오류코드 : 파일 생성 실패 
	 */	
	public static final int ERROR_FILE_CREAT = 100;
	
	/**
	 * 오류코드 : 파일 쓰기 실패 
	 */
	public static final int ERROR_FILE_WRITE = 101;
	
	/**
	 * 오류코드 : 파일 패킷 건수가 상이함 (패킷 손실 ) 
	 */
	public static final int ERROR_MISMATCH_RECV_CNT = 102;
	
	/**
	 * 오류코드 : 해당 파일 없음 (물리적 파일 열기 실패) 
	 */
	public static final int ERROR_FILE_OPEN = 103;
	
	/**
	 * 오류코드 : 해당 파일 없음 (파일 목록 조회 실패) 
	 */
	public static final int ERROR_GET_FILENAME = 104;
	
	/**
	 * 오류코드 : KSNET 파일 관리 DB에러 
	 */
	public static final int ERROR_FILE_INSERT_DB = 105;
	
	/**
	 * 오류코드 : 파일 읽기 실패 
	 */
	public static final int ERROR_FILE_READ = 106;
	
	/**
	 * 오류코드 : 요청일자 틀림. (업로드시 일자는 항상 "오늘" 입니다.)
	 */
	public static final int ERROR_REQTDATE_SMALL = 107;
	
	/**
	 * 오류코드 : 미등록 상점아이디(또는 그룹아이디)
	 */
	public static final int ERROR_NOT_REGISTER_STORE = 108;
	
	/**
	 * 오류코드 : 전문레이아웃 오류  
	 */
	public static final int ERROR_MSG_FORMAT = 111;/* 데이타 전문오류 */
	
	
	private static final int ERROR_MSG_CRC = 112; /* CRC오류 */
	// -- [E] 오류코드 정의 ---------------------------------------------
	private static final int CONN_TIMEOUT = 3000;
	private static final int TIMEOUT = 30000; // 30seconds.
	private static final byte UPLOAD = 0x31; /* '1' */
	private static final byte DOWNLOAD = 0x32; /* '2'; */
	private static final String JAVA_CRC_DEFAULT = "CL"; 

	private static final int MSG_LENGTH_SIZE = 6;
	private static final int MAXBUFSIZE = 4096 - MSG_LENGTH_SIZE;
	private static final int FILEBUFSIZE = MAXBUFSIZE - PG_FTS_HEADER.SIZE;

	private static PrintStream KSNOUT = System.out;

	private Socket socket = null;
	private InetSocketAddress destAddr = null;
	private int destPort = -1;

	private DataInputStream din = null;
	private DataOutputStream dout = null;

	private DataInputStream file_in = null;
	private DataOutputStream file_out = null;

	private boolean isAllClosed = false;

	PG_FTS_HEADER send_header_stx;
	PG_FTS_BODY_STARTMSG send_body_stx;
	PG_FTS_HEADER recvheader_stx;
	PG_FTS_BODY_RES recv_body_stx;

	private int ks_err_code;

	private byte[] enc_rsaseed_key;
	private byte[] rsaseed_key;

	private long g_filesize; /* 파일 사이즈 */
	private long g_accum_datasize; /* 다운로드 데이타 누적 사이즈 */

	// [E] KSNET 정의 영역
	// ////////////////////////////////////////////////////////////

	public KSPGFtsUpDownLib()
	{
		// 절대 static 변수를 여기서 초기화 하지 말것!!!!

		send_header_stx = new PG_FTS_HEADER();
		send_body_stx = new PG_FTS_BODY_STARTMSG();
		recvheader_stx = new PG_FTS_HEADER();
		recv_body_stx = new PG_FTS_BODY_RES();

		ks_err_code = SUCCESS;		
	}

	/**
	 * 로그파일 경로(디렉토리 경로 + 파일명) 설정 메소드
	 * 
	 * @param out
	 *            로그 PrintStream 지정.  기본 : 표준 출력      null : 로그 출력 없음
	 */
	public static void setLogPrintStream(PrintStream out)
	{
		KSNOUT = out;	
	}

	/**
	 * 로그레벨 설정 메소드; 
	 * 
	 * @param loglevel
	 *            로그레벨 값 1~4  (상수값 참조)
	 *            기본값 : LOG_ERROR(1)
	 
	 *  
	 */
	public static void setLogLevel(int loglevel)
	{
		if (loglevel < LOG_ERROR)
			loglevel = LOG_ERROR;
		LOG_LEVEL = loglevel;
	}

	/**
	 * @param host
	 *            : IP Adress ex) "210.181.28.137"
	 * @param port
	 *            : Port Number ex) 9800
	 * @param path
	 *            : Full Path of a file to save ex)
	 *            "C : /WORKs/매입결과/ksnet.20070129.down.txt"
	 * @param busDf
	 *            : KSNET 매입 담당자 안내 참조 ex) "PGEDI"
	 * @param allOrNotRcv
	 *            : 미수신 파일만 받기 구분 0-모두 (권장값), 1-미수신파일만 ex) "0"
	 * @param shopId
	 *            : 상점 ID ex) "2999199999"
	 * @param enc_shop_pass
	 *            :  API 사용 key값  (일조의 암호 역할 ) - KSNET 담당자로부터 발급받으세요      
	 * @param date
	 *            : 받고자 하는 파일의 생성일자(매입결과파일의 경우 매입결과일) ex) "20070125"
	 * @return 성공:0 실패 :나머지
	 * 
	 *         <pre>
	 *  <li>실행 환경은 JRE 1.5.x 이상입니다.</li>
	 * </pre>
	 */
	public static int fileDownload(String host, int port, String path,
			String busDf, String allOrNotRcv, String shopId,String enc_shop_pass ,String date)
	{
		WriteLog(
				LOG_INFO,
				String.format(
						"[FileDownload]host:%s, port:%d, path:%s, busDf:%s, shop_id:%s, date:%s",
						host, port, path, busDf, shopId, date));

		int rtn = 0;
		KSPGFtsUpDownLib sfh = new KSPGFtsUpDownLib();
		try
		{
			rtn = sfh.downloadFile(host, port, path, busDf, allOrNotRcv,
					shopId, enc_shop_pass,date);
			if (rtn >= 0)
				rtn = 0;
			WriteLog(LOG_INFO, "[FileDownload]종료응답코드: " + rtn);

		} catch (Exception e)
		{
			WriteLog(LOG_ERROR, "[FileDownload] " + e.toString());
			return -1;
		}
		sfh.closeAll();

		return rtn;
	}

	/**
	 * @param host
	 *            : IP Adress ex) "210.181.28.137"
	 * @param port
	 *            : Port Number ex) 9800
	 * @param path
	 *            : Full Path of a file to save ex)
	 *            "C : /WORKs/매입결과/ksnet.20070129.down.txt"
	 * @param busDf
	 *            : KSNET 매입 담당자 안내 참조 ex) "PGEDI"
	 * @param shopId
	 *            : 상점 ID ex) "2999199999"
	 * @param enc_shop_pass
	 *            : API 사용 key값  (일조의 암호 역할 ) - KSNET 담당자로부터 발급받으세요              
	 * @param date
	 *            : 받고자 하는 파일의 생성일자(매입결과파일의 경우 매입결과일) ex) "20170125"
	 * @return 성공:0 실패 :나머지
	 * 
	 *         <pre>
	 *  <li>실행 환경은 JRE 1.5.x 이상입니다.</li>
	 * </pre>
	 * 
	 */
	public static int fileUpload(String host, int port, String path,
			String busDf, String shopId, String enc_shop_pass ,String date)
	{

		WriteLog(
				LOG_INFO,
				String.format(
						"[FileUpload]host:%s, port:%d, path:%s, busDf:%s, shop_id:%s, date:%s",
						host, port, path, busDf, shopId, date));

		KSPGFtsUpDownLib sfh = new KSPGFtsUpDownLib();
		int rtn = 0;
		try
		{
			rtn = sfh.uploadFile(host, port, path, busDf, shopId, enc_shop_pass, date);
			if (rtn >= 0)
				rtn = 0;
			WriteLog(LOG_INFO, "[FileUpload]종료응답코드: " + rtn);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[FileUpload] " + e.toString());
			return -1;
		}
		sfh.closeAll();

		return rtn;
	}

	private int uploadFile(String host, int port, String path, String busDf,
			String shopId,String enc_shop_pass , String work_date)
	{
		int rtn = 0;

		DataInputStream fin = null;
		byte frbuf[] = new byte[FILEBUFSIZE];
		PG_FTS_HEADER s_header = new PG_FTS_HEADER();
		PG_FTS_HEADER r_header;
		PG_FTS_BODY_RES r_body;
		int read_len = 0;
		int snd_cnt = 0;
		byte send_buf[] = new byte[MAXBUFSIZE];
		RDATA rdata = new RDATA();

		try
		{
			/* STEP 1. 초기화 및 서버접속 */
			initHandler(host, port);
			WriteLog(LOG_INFO,
					"[UPLOAD] 서버 접속 성공!! [" + host + ":" + String.valueOf(port)
							+ "]\n");

			/* STEP 2. 암호키 생성 및 서버로 전송 */
			if (false == GetAndSendSeedKey())
			{
				WriteLog(LOG_ERROR, "[UPLOAD] 암호화키 생성 및 전송 실패!!\n");
				return -1;
			}
			WriteLog(LOG_DEBUG, "[UPLOAD] 암호화키 생성 및 전송 성공!!\n");

			/* STEP 3. 시작전문 송수신 */
			rtn = ProcStartMsg(UPLOAD, shopId, enc_shop_pass ,busDf, (byte) '0', work_date,
					path);
			if (rtn < 0)
			{
				WriteLog(LOG_ERROR, "[UPLOAD] 시작전문 송수신  실패!!\n");
				return -1;
			}
			WriteLog(LOG_INFO, "[UPLOAD] 시작전문 송수신 성공!!\n");

			s_header = this.send_header_stx;
			s_header.msg_type[0] = 'D';

			int idx = 0;
			/* STEP 4 파일 읽기 */
			fin = new DataInputStream(new FileInputStream(path));

			KSStrByteUtil.initBytes(frbuf, (byte) 0x00);
			while ((read_len = fin.read(frbuf, 0, FILEBUFSIZE)) > 0)
			{
				KSStrByteUtil.initBytes(send_buf, (byte) 0x00);
				snd_cnt++;
				System.arraycopy(String.format("%08d", snd_cnt).getBytes(), 0,
						s_header.msg_seq, 0, 8);

				idx = 0;
				System.arraycopy(s_header.getBytes(), 0, send_buf, idx,
						PG_FTS_HEADER.SIZE);
				idx = idx + PG_FTS_HEADER.SIZE;
				System.arraycopy(frbuf, 0, send_buf, idx, read_len);
				;
				idx = idx + read_len;

				/* STEP 5 파일 송신 */
				rtn = SendMsg(send_buf, PG_FTS_HEADER.SIZE + read_len);
				if (rtn < 0)
				{
					WriteLog(LOG_ERROR, "[UPLOAD] 데이타 전송 실패!! \n");
					break;
				}

				/* STEP 6 데이타 응답전문 */
				rdata = RecvMsg();
				WriteLog(LOG_DEBUG, "[UPLOAD] recv_buf:[" + rdata.toString()
						+ "]\n");

				int recv_len = rdata.len;
				WriteLog(
						LOG_DEBUG,
						"[UPLOAD] 응답 수신데이타 길이 recv_len:["
								+ String.valueOf(recv_len) + "]\n");
				if (recv_len != PG_FTS_HEADER.SIZE + PG_FTS_BODY_RES.SIZE)
				{
					WriteLog(LOG_ERROR, "[UPLOAD] 응답 데이타 길이 오류!!  \n");
					rtn = -1;
					break;
				}

				byte[] r_buf_header = new byte[PG_FTS_HEADER.SIZE];
				byte[] r_buf_body = new byte[PG_FTS_BODY_RES.SIZE];

				idx = 0;
				System.arraycopy(rdata.data, idx, r_buf_header, 0,
						PG_FTS_HEADER.SIZE);
				idx = idx + PG_FTS_HEADER.SIZE;
				System.arraycopy(rdata.data, idx, r_buf_body, 0,
						PG_FTS_BODY_RES.SIZE);
				idx = idx + PG_FTS_BODY_RES.SIZE;

				r_header = new PG_FTS_HEADER(r_buf_header);
				r_body = new PG_FTS_BODY_RES(r_buf_body);

				if (r_header.msg_type[0] == 'E')
				{
					WriteLog(LOG_ERROR, "[UPLOAD] 오류발생!! 오류코드:["
							+ new String(r_body.resp_code) + "]\n");
					rtn = -1;
					break;
				}

				// 다시 초기화
				KSStrByteUtil.initBytes(frbuf, (byte) 0x00);
			}

			/* STEP 7 종료요청 */
			idx = 0;
			rtn = Ack2Server((byte) 'F', s_header, SUCCESS);
			if (rtn < 0)
			{
				WriteLog(LOG_ERROR, "[UPLOAD]  종료 요청 실패!! \n");
				return -1;
			}

			/* STEP 8 종료 응답 */
			KSStrByteUtil.initBytes(rdata.data, (byte) 0x00);
			rdata.len = 0;
			rdata = RecvMsg();
			WriteLog(LOG_DEBUG, "[UPLOAD] 종료전문 수신:[" + rdata.toString() + "]\n");

			int recv_len = rdata.len;
			WriteLog(
					LOG_DEBUG,
					"[UPLOAD] 응답 수신데이타 길이 recv_len:["
							+ String.valueOf(recv_len) + "]\n");

			byte[] r_buf_header = new byte[PG_FTS_HEADER.SIZE];
			byte[] r_buf_body = new byte[PG_FTS_BODY_RES.SIZE];

			idx = 0;
			System.arraycopy(rdata.data, idx, r_buf_header, 0,
					PG_FTS_HEADER.SIZE);
			idx = idx + PG_FTS_HEADER.SIZE;
			System.arraycopy(rdata.data, idx, r_buf_body, 0,
					PG_FTS_BODY_RES.SIZE);
			idx = idx + PG_FTS_BODY_RES.SIZE;

			r_header = new PG_FTS_HEADER(r_buf_header);
			r_body = new PG_FTS_BODY_RES(r_buf_body);

			if (r_header.msg_type[0] == 'E')
			{
				WriteLog(LOG_ERROR, "[UPLOAD] 오류발생!! 오류코드:["
						+ new String(r_body.resp_code) + "]\n");
				return -1;
			}

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[UPLOAD]" + e.toString());
			return -1;
		} catch (KSMyException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[UPLOAD]" + e.toString());
			return -1;
		}

		return rtn;
	}

	private int downloadFile(String host, int port, String path, String busDf,
			String allOrNotRcv, String shopId, String enc_shop_pass, String work_date)
	{
		int rtn = 0;

		DataOutputStream fout = null;
		PG_FTS_HEADER s_header = new PG_FTS_HEADER();
		PG_FTS_HEADER r_header;
		PG_FTS_BODY_RES r_body;
		int read_len = 0;
		int snd_cnt = 0;
		byte send_buf[] = new byte[MAXBUFSIZE];
		RDATA rdata = new RDATA();
		File f1 = null;

		g_accum_datasize = 0;

		try
		{
			/* STEP 1. 초기화 및 서버접속 */
			initHandler(host, port);
			WriteLog(
					LOG_INFO,
					"[DOWNLOAD] 서버 접속 성공!! [" + host + ":"
							+ String.valueOf(port) + "]\n");

			/* STEP 2. 암호키 생성 및 서버로 전송 */
			if (false == GetAndSendSeedKey())
			{
				WriteLog(LOG_ERROR, "[DOWNLOAD] 암호화키 생성 및 전송 실패!!\n");
				return -1;
			}
			WriteLog(LOG_DEBUG, "[DOWNLOAD] 암호화키 생성 및 전송 성공!!\n");

			/* STEP 3. 시작전문 송수신 */
			byte b_allOrNotRcv [] = new byte [1];
			System.arraycopy(allOrNotRcv.getBytes(), 0,b_allOrNotRcv  , 0, 1);
			rtn = ProcStartMsg(DOWNLOAD, shopId, enc_shop_pass,busDf, b_allOrNotRcv[0], work_date,
					path);
			if (rtn < 0)
			{
				WriteLog(LOG_ERROR, "[DOWNLOAD] 시작전문 송수신  실패!!");
				return -1;
			}
			WriteLog(LOG_INFO, "[DOWNLOAD] 시작전문 송수신 성공!!");

			s_header = this.send_header_stx;

			int idx = 0;

			/* STEP 4 파일 저장 준비 */
			fout = new DataOutputStream(new FileOutputStream(path));
			f1 = new File(path);

			/* 데이타 수신 loop */
			while (true)
			{

				/* STEP 5. 데이타 수신 */
				rdata = RecvMsg();

				WriteLog(LOG_DEBUG, "[DOWNLOAD] received:[" + rdata.toString()
						+ "]");

				byte[] r_buf_header = new byte[PG_FTS_HEADER.SIZE];
				byte[] frbuf = new byte[FILEBUFSIZE];
				byte[] r_buf_body = new byte[PG_FTS_BODY_RES.SIZE];

				idx = 0;
				System.arraycopy(rdata.data, 0, r_buf_header, idx,
						PG_FTS_HEADER.SIZE);
				idx = idx + PG_FTS_HEADER.SIZE;

				r_header = new PG_FTS_HEADER(r_buf_header);

				if (r_header.msg_type[0] == 'D') /* 파일 데이타 */
				{
					System.arraycopy(rdata.data, idx, frbuf, 0, rdata.len
							- PG_FTS_HEADER.SIZE);
					fout.write(frbuf, 0, rdata.len - PG_FTS_HEADER.SIZE);
					g_accum_datasize = g_accum_datasize
							+ (rdata.len - PG_FTS_HEADER.SIZE);

					// 데이타 수신에 대한 응답
					rtn = Ack2Server((byte) 'R', r_header, SUCCESS);
					if (rtn < 0)
					{
						WriteLog(LOG_ERROR, "[DOWNLOAD]  종료 응답 송신 실패!! \n");
						f1.delete(); // 파일 삭제
						return -1;
					}
				} else
				{
					System.arraycopy(rdata.data, idx, r_buf_body, 0, rdata.len
							- PG_FTS_HEADER.SIZE);
					r_body = new PG_FTS_BODY_RES(r_buf_body);

					if (r_header.msg_type[0] == (byte) 'F') /* 정상종료 */
					{
						WriteLog(LOG_INFO, "[DOWNLOAD] 파일 다운로드 정상종료 수신  \n");

						/* STEP 7 종료요청 */
						rtn = Ack2Server((byte) 'F', r_header, SUCCESS);
						if (rtn < 0)
						{
							WriteLog(LOG_ERROR, "[DOWNLOAD]  종료 응답 송신 실패!! \n");
							// return -1;
						}
					} else
					{
						WriteLog(LOG_ERROR, "[DOWNLOAD] 오류 전문 수신 !! 오류코드:["
								+ new String(r_body.resp_code, 0, 4) + "]\n");
						f1.delete();
						return -1;
					}

					break;
				}
				r_body = null;
				r_header = null;
				r_buf_body = null;

			}

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[DOWNLOAD]" + e.toString());
			if (f1 != null)
				f1.delete();
			if (0 > Ack2Server((byte) 'E', s_header, ERROR_FILE_WRITE))
				WriteLog(LOG_ERROR, "[DOWNLOAD]  오류전문 송신 실패!! \n");

			return -1;
		} catch (KSMyException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[DOWNLOAD]" + e.toString());
			if (f1 != null)
				f1.delete();
			if (0 > Ack2Server((byte) 'E', s_header, 999))
				WriteLog(LOG_ERROR, "[DOWNLOAD]  오류전문 송신 실패!! \n");
			return -1;
		} catch (Exception e)
		{
			WriteLog(LOG_ERROR, "[DOWNLOAD]" + e.toString());
			if (f1 != null)
				f1.delete();
			if (0 > Ack2Server((byte) 'E', s_header, 999))
				WriteLog(LOG_ERROR, "[DOWNLOAD]  오류전문 송신 실패!! \n");
			return -1;
		}

		return rtn;
	}

	/**
	 * 암호화 KEY를 생성해서 서버에 보냄
	 * 
	 * @return
	 */
	private boolean GetAndSendSeedKey()
	{
		rsaseed_key = KSStrByteUtil.s2b(new StringBuffer()
				.append(System.currentTimeMillis()).append(Long.MAX_VALUE)
				.substring(0, 16));
		try
		{
			enc_rsaseed_key = KSPayFEnc.ks_rsa_encrypt(rsaseed_key);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[GetAndSendSeedKey]" + e.toString());
			return false;
		}
		WriteLog(LOG_DEBUG, "[GetAndSendSeedKey]rsa encrypted key : "
				+ enc_rsaseed_key);

		/* sprintf(buf, "%06d%c", len + 1,ENCRYPT_TYPE); */
		byte[] send_byte = new byte[PACKET_SIZE];

		send_byte[0] = ENCRYPT_TYPE;
		System.arraycopy(enc_rsaseed_key, 0, send_byte, 1, 128);

		int rtn = SendMsg(send_byte, 129, false);
		WriteLog(LOG_DEBUG,
				"[GetAndSendSeedKey]send rtn  : " + String.valueOf(rtn));
		if (rtn < 0)
		{
			WriteLog(LOG_ERROR, "암호키 서버 전송 실패 \n");
			return false;
		}

		return true;
	}

	/**
	 * 시작전문 송수신
	 * 
	 * @return
	 */
	private int ProcStartMsg(byte updown, String shop_id, String enc_shop_pass, String busDF,
			byte AllOrNotRcv, String work_date, String filename)
	{
		int rtn = 0;
		int idx = 0;

		RDATA rdata = new RDATA();

		/* 구버전과의 호환을 위해 업무구분 3자리일 경우 "PG"를 앞에 붙여 5자리로 변형 */
		if (busDF.length() == 3)
		{
			WriteLog(LOG_DEBUG, "업무구분(busDF) 변형 : [" + busDF + "] => [PG"
					+ busDF + "]\n");
			busDF = "PG" + busDF;
		}		
		

		String timestamp = getNowTime("yyyyMMdd") + getNowTime("hhmmss");
		
		
		/* header */
		send_header_stx.ecy_cd[0] = ENCRYPT_TYPE;
		System.arraycopy("V02".getBytes(), 0, send_header_stx.ver_busi_sele, 0,	3);
		send_header_stx.updwondf[0] = updown;
		send_header_stx.filedownflag[0] = AllOrNotRcv;
		System.arraycopy(shop_id.getBytes(), 0, send_header_stx.mid, 0, 10);
		send_header_stx.msg_type[0] = 'S';
		System.arraycopy(busDF.getBytes(), 0, send_header_stx.busDF, 0, 5);
		KSStrByteUtil.initBytes(send_header_stx.msg_seq, (byte) '0');

		/* body */
		System.arraycopy(timestamp.getBytes(), 0, send_body_stx.timestamp, 0,14);
		
		/* API 사용 키 enc_shop_pass */
		String reqhash = enc_shop_pass;
		WriteLog(LOG_DEBUG,"reqhash(enc_shop_pass):[" + reqhash  + "]\n"  );
		System.arraycopy(reqhash.getBytes(),0 ,send_body_stx.reqhash , 0, reqhash.length());
		WriteLog(LOG_DEBUG,"work_date:[" + work_date  + "]\n"  );
		System.arraycopy(work_date.getBytes(), 0, send_body_stx.obj_date, 0, 8);
		KSStrByteUtil.initBytes(send_body_stx.filesize, (byte) '0');
		send_body_stx.fname_mng[0] = '0';
		send_body_stx.file_cps[0]  = '0';
		KSStrByteUtil.initBytes(send_body_stx.filler1, (byte) ' ');
		KSStrByteUtil.initBytes(send_body_stx.filler2, (byte) ' ');

		/* 파일사이즈 */
		if (updown == UPLOAD)
		{
			long fsize = GetFileSize(filename);
			if (fsize < 0)
			{
				ks_err_code = ERROR_FILE_OPEN;
				WriteLog(LOG_ERROR, "파일:[" + filename + "] (이)가 없음. 확인요망!!  \n");
				return -1;
			}
			g_filesize = fsize;
			WriteLog(LOG_INFO,
					"FILE:[" + filename + "] SIZE:" + String.valueOf(fsize)
							+ "bytes \n");
			System.arraycopy(String.format("%010d", fsize).getBytes(), 0,
					send_body_stx.filesize, 0, 10);
		}

		idx = 0;
		byte send_buf[] = new byte[PG_FTS_HEADER.SIZE
				+ PG_FTS_BODY_STARTMSG.SIZE];
		System.arraycopy(send_header_stx.getBytes(), 0, send_buf, idx,
				PG_FTS_HEADER.SIZE);
		idx = idx + PG_FTS_HEADER.SIZE;
		System.arraycopy(send_body_stx.getBytes(), 0, send_buf, idx,
				PG_FTS_BODY_STARTMSG.SIZE);
		idx = idx + PG_FTS_BODY_STARTMSG.SIZE;
		WriteLog(LOG_DEBUG, "[시작전문] 요청 >> [" + new String(send_buf) + "]");
		rtn = SendMsg(send_buf, PG_FTS_HEADER.SIZE + PG_FTS_BODY_STARTMSG.SIZE);
		if (rtn < 0)
		{
			WriteLog(LOG_ERROR, "[시작전문]  요청 실패!! \n");
			return -1;
		}

		try
		{
			rdata = RecvMsg();

			byte[] recv_buf_header = new byte[PG_FTS_HEADER.SIZE];
			byte[] recv_buf_body = new byte[PG_FTS_BODY_STARTMSG.SIZE];

			idx = 0;
			System.arraycopy(rdata.data, idx, recv_buf_header, 0,
					PG_FTS_HEADER.SIZE);
			idx = idx + PG_FTS_HEADER.SIZE;
			System.arraycopy(rdata.data, idx, recv_buf_body, 0,
					PG_FTS_BODY_STARTMSG.SIZE);
			idx = idx + PG_FTS_BODY_STARTMSG.SIZE;

			PG_FTS_HEADER r_header = new PG_FTS_HEADER(recv_buf_header);
			PG_FTS_BODY_STARTMSG r_sbody = new PG_FTS_BODY_STARTMSG(
					recv_buf_body);
			PG_FTS_BODY_RES r_body = new PG_FTS_BODY_RES(recv_buf_body);

			WriteLog(LOG_DEBUG, "[시작전문] 응답  >> [" + rdata.data.toString()	+ "]\n");

			if (r_header.msg_type[0] == 'S')
			{
				WriteLog(LOG_DEBUG, "[시작전문] 응답 성공");

				/* 업로드 외에... */
				if (updown != UPLOAD)
				{
					g_filesize = Long.parseLong(new String(r_sbody.filesize, 0,	10));
				}

			} else if (r_header.msg_type[0] == 'E')
			{
				String resp_code = new String(r_body.resp_code);
				WriteLog(LOG_ERROR, "[시작전문]  응답 실패!!   오류코드 :[" + resp_code
						+ "]");
				ks_err_code = Integer.parseInt(resp_code);
				return -1;
			} else
			{
				WriteLog(LOG_ERROR, "[시작전문]  응답 실패 (알 수 없는 응답 전문)!! MSG_TYPE:["
						+ new String(r_header.msg_type) + "]\n");
				return -1;
			}

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, e.toString());
			return -1;
		} catch (KSMyException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, e.toString());
			return -1;
		}

		return rtn;
	}

	private void initHandler(String host, int port) throws IOException
	{
		try
		{

			socket = new Socket();
			socket.setSoTimeout(TIMEOUT);
			socket.setReuseAddress(true);
			socket.setOOBInline(true);
			socket.setKeepAlive(true);
			socket.setSoLinger(true, TIMEOUT);
			destPort = port;
			destAddr = new InetSocketAddress(InetAddress.getByName(host), port);
			socket.connect(destAddr, CONN_TIMEOUT);

			dout = new DataOutputStream(socket.getOutputStream());
			din = new DataInputStream(socket.getInputStream());

		} catch (Exception e)
		{
			closeAll();
			WriteLog(LOG_ERROR, "[initHandler] >> \n" + e.toString());
		}
		WriteLog(LOG_INFO, "Connected to " + host + ":" + String.valueOf(port));
	}

	/* 소켓 */
	private void closeAll()
	{
		KSNOUT.flush();
		if (isAllClosed)
			return;
		try
		{
			if (din != null)
				din.close();
			if (dout != null)
				dout.close();
			if (socket != null)
				socket.close();
			isAllClosed = true;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, e.toString());
		}
	}

	private int Ack2Server(byte msg_type, PG_FTS_HEADER s_header, int err_code)
	{
		int idx = 0;
		byte send_buf[] = new byte[PG_FTS_HEADER.SIZE + PG_FTS_BODY_RES.SIZE];
		PG_FTS_BODY_RES s_body = new PG_FTS_BODY_RES();

		s_header.msg_type[0] = msg_type;
		String.format("%04d", err_code).getBytes();

		KSStrByteUtil.initBytes(send_buf, (byte) 0x20);
		System.arraycopy(s_header.getBytes(), 0, send_buf, idx,
				PG_FTS_HEADER.SIZE);
		idx = idx + PG_FTS_HEADER.SIZE;
		System.arraycopy(s_body.getBytes(), 0, send_buf, idx,
				PG_FTS_BODY_RES.SIZE);

		WriteLog(LOG_DEBUG, "[Ack2Server] 종료전문 요청:[" + new String(send_buf)
				+ "]\n");
		return SendMsg(send_buf, PG_FTS_HEADER.SIZE + PG_FTS_BODY_RES.SIZE,
				true);
	}

	/** 전문길이 6bytes + 전문 송신 */
	private int SendMsg(byte[] send_data, int len)
	{
		return SendMsg(send_data, len, true);
	}

	private int SendMsg(byte[] send_data, int len, boolean bool_enc)
	{
		byte[] send_byte = new byte[8192];

		/* CRC 생성을 위한 데이타 부분만 임시 버퍼 복사 */

		if (bool_enc == true)
		{
			send_data[48] = (byte) 'C';
			send_data[49] = (byte) 'L';

			// 암호화 하기 위해서 임시 버퍼에 복사\
			byte[] buf2 = new byte[len - 1];
			System.arraycopy(send_data, 1, buf2, 0, len - 1);

			// 암호화

			byte[] enc_byte;
			try
			{
				enc_byte = KSPayFEnc.ks_seed_encrypt(rsaseed_key, buf2);

				len = enc_byte.length + 1;
				System.arraycopy(String.format("%06d", len).getBytes(), 0,
						send_byte, 0, 6);
				send_byte[6] = send_data[0];
				System.arraycopy(enc_byte, 0, send_byte, 7, enc_byte.length);

			} catch (InvalidKeyException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (NoSuchProviderException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (BadPaddingException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (NoSuchPaddingException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (InvalidAlgorithmParameterException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (NoSuchAlgorithmException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			} catch (IllegalBlockSizeException e)
			{
				// TODO Auto-generated catch block
				WriteLog(LOG_ERROR, e.toString());
				return -1;
			}

		} else
		/* 암호화 키 보낼 때는 CRC 와 암호호 작업 필요 없음 */
		{
			System.arraycopy(String.format("%06d", len).getBytes(), 0,
					send_byte, 0, 6);
			System.arraycopy(send_data, 0, send_byte, 6, len);
		}

		try
		{
			dout.write(send_byte, 0, len + 6);
			dout.flush();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, e.toString());
			return -1;
		}

		return len;
	}

	/* 전문길이 6bytes + 전문 수신 */
	private RDATA RecvMsg() throws IOException, KSMyException
	{
		byte[] rbuf = new byte[8192];
		byte[] lenbuf = new byte[6];
		byte[] r_encdf = new byte[1];
		int msg_len = 0;
		int rtn_len = 0;
		int read_len = 0;

		RDATA rdata = new RDATA();

		rtn_len = din.read(lenbuf, 0, 6);
		if (rtn_len != 6)
		{
			WriteLog(LOG_ERROR, "전문길이 읽기 실패");
			throw new KSMyException("전문길이 읽기 실패!!");
		}

		msg_len = Integer.parseInt(new String(lenbuf, 0, 6));

		int t_read_len = 0;
		for (read_len = 0; read_len < msg_len;)
		{
			t_read_len = din.read(rbuf, read_len, 1024);
			if (t_read_len <= 0)
			{
				WriteLog(LOG_ERROR, "전문 읽기 실패");
				throw new KSMyException("전문길이 읽기 실패!!");
			}
			read_len = read_len + t_read_len;

		}
		byte[] decBuf;
		try
		{
			byte rrbuf[] = new byte[read_len - 1];
			System.arraycopy(rbuf, 1, rrbuf, 0, read_len - 1);

			decBuf = KSPayFEnc.ks_seed_decrypt(rsaseed_key, rrbuf);

			WriteLog(LOG_DEBUG, "[수신데이타 복호화]" + new String(decBuf));

			int dec_len = decBuf.length;
			rdata.data[0] = rbuf[0];

			System.arraycopy(decBuf, 0, rdata.data, 1, dec_len);
			rdata.len = dec_len + 1;

		} catch (InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg1]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (NoSuchProviderException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg2]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (NoSuchPaddingException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg3]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (InvalidAlgorithmParameterException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg4]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (BadPaddingException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg5]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg6]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (IllegalBlockSizeException e)
		{
			// TODO Auto-generated catch block
			WriteLog(LOG_ERROR, "[RecvMsg7]" + e.toString());
			throw new KSMyException(e.toString());
		} catch (Exception e)
		{
			WriteLog(LOG_ERROR, "[RecvMsg_ETC]" + e.toString());
		}

		return rdata;
	}
	
	

	private static void WriteLog(int log_level, String msg)
	{
		if (KSNOUT == null)
			return;

		// 로그레벨에 따른 출력 조정
		if (LOG_LEVEL < log_level)
			return;

		// 로그 종류 문자열
		String lvlstr = new String();
		switch (log_level)
		{
		case LOG_DEFAULT:
			lvlstr = "";
			break;
		case LOG_ERROR:
			lvlstr = "<ERROR>";
			break;
		case LOG_WARNING:
			lvlstr = "<WARNING>";
			break;
		case LOG_INFO:
			lvlstr = "<INFO>";
			break;
		default:
			lvlstr = "<DEBUG>";
		}

		String ts = getNowTime("yyyyMMdd") + "." + getNowTime("hhmmss");
//		KSNOUT.println("[" + ts + "]" + lvlstr + msg);
		logger.info("[" + ts + "]" + lvlstr + msg);
	}

	private static long GetFileSize(String filename)
	{
		long L = -1;

		File oFile = new File(filename);
		if (oFile.exists())
		{
			L = oFile.length();
		} else
		{
			WriteLog(LOG_ERROR, "해당 파일 없음 : [" + filename + "]\n");
		}

		return L;
	}

	private static String getNowTime(String type)
	{
		// yyyy : 연도
		// MM : 월
		// dd : 일자
		// hh : 시
		// mm : 분
		// ss : 초
		if (type == null || type.equals(""))
		{
			type = "yyyyMMddhhmmss";
		}
		GregorianCalendar calendar = new GregorianCalendar();
		// SimpleDateFormat dateFormat = new
		// SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
		SimpleDateFormat dateFormat = new SimpleDateFormat(type);
		String str = dateFormat.format(calendar.getTime());
		return str;
	}

	class RDATA
	{
		public byte data[];
		public int len;

		public RDATA()
		{
			data = new byte[8192];
			len = 0;
		}

		public String toString()
		{
			return new String(data);
		}
	}

	/** [S] 전문 structure *********************************************************************************************************/
	class PG_FTS_HEADER
	{
		public byte ecy_cd[]; /* size : 1 *//* 1.암호화구분 >> '0':평문 '2':암호화 */
		public byte ver_busi_sele[]; /* size : 3 *//* 2.버전 >> 'V02' */
		public byte updwondf[]; /* size : 1 *//*
											 * 3.up/down 구분 >> '1':upload
											 * '2':download
											 */
		public byte filedownflag[]; /* size : 1 *//*
												 * 4. 다운로드 대상>> '0':기본(전체)
												 * '2':다운로드 한번도 받은적 없는 건만 --
												 * 가능하면 '2'는 사용?
												 */
		public byte mid[]; /* 5. 아이디 >> 일반적으로 상점아이디 */
		public byte msg_type[]; /* size : 1 *//*
											 * 6.메시지 종류 >> S :시작전문요청/응답,
											 * D:데이타전문전송 F:종료전문요청/응답 R:데이타전문?
											 */
		public byte busDF[]; /* 7.업무구분 >> *문서참조 */
		public byte msg_seq[]; /* size : 8 *//* 8.메시지 순번 >> 시작전문은 0번 */
		public byte user_def_numb[]; /* size : 12 *//*
													 * 9. 사용자 정의값>> 시작전문 요청시
													 * setting 유지
													 */
		public byte filler[]; /* size : 4 *//* 10. filler */
		public byte msg_type2[]; /* 어쩔수 없이 만든 잉여 msg_type */
		public byte sender[]; /* size : 1 *//* 전송자 구분 "C":클라이언트 "K":서버(KSNET) */
		public byte crc[]; /* size : 2 *//* 12. BODY부분 또는 파일데이타 부분 CRC 값 */

		public static final int SIZE = 50;

		public PG_FTS_HEADER()
		{
			ecy_cd = new byte[1];
			ver_busi_sele = new byte[3];
			updwondf = new byte[1];
			filedownflag = new byte[1];
			mid = new byte[10];
			msg_type = new byte[1];
			busDF = new byte[5];
			msg_seq = new byte[8];
			user_def_numb = new byte[12];
			filler = new byte[4];
			msg_type2 = new byte[1];
			sender = new byte[1];
			crc = new byte[2];

			KSStrByteUtil.initBytes(ecy_cd, (byte) 0x20);
			KSStrByteUtil.initBytes(ver_busi_sele, (byte) 0x20);
			KSStrByteUtil.initBytes(updwondf, (byte) 0x20);
			KSStrByteUtil.initBytes(filedownflag, (byte) 0x20);
			KSStrByteUtil.initBytes(mid, (byte) 0x20);
			KSStrByteUtil.initBytes(msg_type, (byte) 0x20);
			KSStrByteUtil.initBytes(busDF, (byte) 0x20);
			KSStrByteUtil.initBytes(msg_seq, (byte) 0x20);
			KSStrByteUtil.initBytes(user_def_numb, (byte) 0x20);
			KSStrByteUtil.initBytes(filler, (byte) 0x20);
			KSStrByteUtil.initBytes(sender, (byte) 'C');
			crc = JAVA_CRC_DEFAULT.getBytes();
		}

		public PG_FTS_HEADER(byte msg[])
		{
			int idx = 0;

			ecy_cd = new byte[1];
			ver_busi_sele = new byte[3];
			updwondf = new byte[1];
			filedownflag = new byte[1];
			mid = new byte[10];
			msg_type = new byte[1];
			busDF = new byte[5];
			msg_seq = new byte[8];
			user_def_numb = new byte[12];
			filler = new byte[4];
			msg_type2 = new byte[1];
			sender = new byte[1];
			crc = new byte[2];

			/* java version은 CRC통일 . */
			/* java버전은 어차피 대외용 버전이기 때문에 암호화를 하므로 CRC 불필요 */
			crc = JAVA_CRC_DEFAULT.getBytes();

			System.arraycopy(msg, idx, ecy_cd, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, ver_busi_sele, 0, 3);
			idx = idx + 3;
			System.arraycopy(msg, idx, updwondf, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, filedownflag, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, mid, 0, 10);
			idx = idx + 10;
			System.arraycopy(msg, idx, msg_type, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, busDF, 0, 5);
			idx = idx + 5;
			System.arraycopy(msg, idx, msg_seq, 0, 8);
			idx = idx + 8;
			System.arraycopy(msg, idx, user_def_numb, 0, 12);
			idx = idx + 12;
			System.arraycopy(msg, idx, filler, 0, 4);
			idx = idx + 4;
			System.arraycopy(msg, idx, msg_type2, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, sender, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, crc, 0, 2);
			idx = idx + 2;
		}

		public byte[] getBytes()
		{
			int idx = 0;
			byte[] rbyte = new byte[SIZE];

			System.arraycopy(ecy_cd, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(ver_busi_sele, 0, rbyte, idx, 3);
			idx = idx + 3;
			System.arraycopy(updwondf, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(filedownflag, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(mid, 0, rbyte, idx, 10);
			idx = idx + 10;
			System.arraycopy(msg_type, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(busDF, 0, rbyte, idx, 5);
			idx = idx + 5;
			System.arraycopy(msg_seq, 0, rbyte, idx, 8);
			idx = idx + 8;
			System.arraycopy(user_def_numb, 0, rbyte, idx, 12);
			idx = idx + 12;
			System.arraycopy(filler, 0, rbyte, idx, 4);
			idx = idx + 4;
			System.arraycopy(msg_type2, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(sender, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(crc, 0, rbyte, idx, 2);
			idx = idx + 2;

			return rbyte;
		}

	};

	class PG_FTS_BODY_STARTMSG /* 요청 응답 전부 사용 */
	{
		public byte timestamp[]; /* size : 14 *//*
												 * 1. 현재 일자+시간 >> 반드시 시작전문 보낼 때
												 * 시간이어야 함!! KSNET과 5분이상 차이날 경우
												 * 오류!! Y
												 */
		public byte reqhash[]; /* size : 50 *//* 2. 인증 해쉬값 >> */
		public byte obj_date[]; /* size : 8 *//* 3. 일자 >> 업로드시 : 현재일자 다운로드시 : 목표일자 */
		public byte fname_mng[]; /* size : 1 */// * 4. 파일명명 규칙 >>
												// 1:S_상점아이디_yyyymmdd.001
												// 2:S상점아이디_<PID>_g_yyyymmddHHMMSS
												// */
		public byte file_cps[]; /* size : 1 *//*
											 * 5. 파일 압축 종류 >> 0:일반
											 * 1:tar+gzip(다운로드 시에만)
											 */
		public byte filesize[]; /* size : 10 *//* 6. 파일 총 길이 >> 1Gbytes 미만 */
		public byte filler1[]; /* size : 192 *//* 7. filler1 */
		public byte filler2[]; /* size : 24 *//* 8. filler2 */

		public static final int SIZE = 300;

		public PG_FTS_BODY_STARTMSG()
		{
			timestamp = new byte[14];
			reqhash = new byte[50];
			obj_date = new byte[8];
			fname_mng = new byte[1];
			file_cps = new byte[1];
			filesize = new byte[10];
			filler1 = new byte[192];
			filler2 = new byte[24];

			KSStrByteUtil.initBytes(timestamp, (byte) 0x20);
			KSStrByteUtil.initBytes(reqhash, (byte) 0x20);
			KSStrByteUtil.initBytes(obj_date, (byte) 0x20);
			KSStrByteUtil.initBytes(fname_mng, (byte) 0x20);
			KSStrByteUtil.initBytes(file_cps, (byte) 0x20);
			KSStrByteUtil.initBytes(filesize, (byte) 0x20);
			KSStrByteUtil.initBytes(filler1, (byte) 0x20);
			KSStrByteUtil.initBytes(filler2, (byte) 0x20);

		}

		public PG_FTS_BODY_STARTMSG(byte msg[])
		{
			int idx = 0;

			timestamp = new byte[14];
			reqhash = new byte[50];
			obj_date = new byte[8];
			fname_mng = new byte[1];
			file_cps = new byte[1];
			filesize = new byte[10];
			filler1 = new byte[192];
			filler2 = new byte[24];

			System.arraycopy(msg, idx, timestamp, 0, 14);
			idx = idx + 14;
			System.arraycopy(msg, idx, reqhash, 0, 50);
			idx = idx + 50;
			System.arraycopy(msg, idx, obj_date, 0, 8);
			idx = idx + 8;
			System.arraycopy(msg, idx, fname_mng, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, file_cps, 0, 1);
			idx = idx + 1;
			System.arraycopy(msg, idx, filesize, 0, 10);
			idx = idx + 10;
			System.arraycopy(msg, idx, filler1, 0, 192);
			idx = idx + 192;
			System.arraycopy(msg, idx, filler2, 0, 24);
			idx = idx + 24;

		}

		public byte[] getBytes()
		{
			int idx = 0;
			byte[] rbyte = new byte[SIZE];

			System.arraycopy(timestamp, 0, rbyte, idx, 14);
			idx = idx + 14;
			System.arraycopy(reqhash, 0, rbyte, idx, 50);
			idx = idx + 50;
			System.arraycopy(obj_date, 0, rbyte, idx, 8);
			idx = idx + 8;
			System.arraycopy(fname_mng, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(file_cps, 0, rbyte, idx, 1);
			idx = idx + 1;
			System.arraycopy(filesize, 0, rbyte, idx, 10);
			idx = idx + 10;
			System.arraycopy(filler1, 0, rbyte, idx, 192);
			idx = idx + 192;
			System.arraycopy(filler2, 0, rbyte, idx, 24);
			idx = idx + 24;

			return rbyte;
		}
	};

	class PG_FTS_BODY_RES /* 종료전문 또는 오류전문 */
	{
		public byte resp_code[]; /* size : 4 *//* 1.응답코드 >> 0000 : 정상 그외 :오류 */
		public byte datasize[]; /* size : 10 *//*
												 * 2. 데이타크기 >> 데이타전문 : 수신받은 데이타
												 * 건별 크기 , 종료전문 : 수신받은 파일전체 사이즈
												 */
		public byte filler[]; /* size : 36 *//* 3. filler */

		public static final int SIZE = 50;

		public PG_FTS_BODY_RES()
		{
			resp_code = new byte[4];
			datasize = new byte[10];
			filler = new byte[36];

			KSStrByteUtil.initBytes(resp_code, (byte) 0x20);
			KSStrByteUtil.initBytes(datasize, (byte) 0x20);
			KSStrByteUtil.initBytes(filler, (byte) 0x20);
		}

		public PG_FTS_BODY_RES(byte msg[])
		{
			int idx = 0;

			resp_code = new byte[4];
			datasize = new byte[10];
			filler = new byte[36];

			System.arraycopy(msg, idx, resp_code, 0, 4);
			idx = idx + 4;
			System.arraycopy(msg, idx, datasize, 0, 10);
			idx = idx + 10;
			System.arraycopy(msg, idx, filler, 0, 36);
			idx = idx + 36;
		}

		public byte[] getBytes()
		{
			int idx = 0;
			byte[] rbyte = new byte[SIZE];

			System.arraycopy(resp_code, 0, rbyte, idx, 4);
			idx = idx + 4;
			System.arraycopy(datasize, 0, rbyte, idx, 10);
			idx = idx + 10;
			System.arraycopy(filler, 0, rbyte, idx, 36);
			idx = idx + 36;

			return rbyte;
		}
	};

	/** [E] 전문 structure *********************************************************************************************************/

}

// ////암호화 송수신 관련
// ////////////////////////////////////////////////////////////////////////////////////////////
class KSPayFEnc
{
	public static byte[] ks_seed_decrypt(byte[] kbuf, byte[] mbuf)
			throws java.security.NoSuchProviderException,
			javax.crypto.NoSuchPaddingException,
			java.security.InvalidAlgorithmParameterException,
			javax.crypto.BadPaddingException,
			java.security.NoSuchAlgorithmException,
			java.security.InvalidKeyException,
			javax.crypto.IllegalBlockSizeException
	{
		byte tdata[] = new KSPaySeed(kbuf).cbc_decrypt(mbuf);
		return tdata;
	}

	public static byte[] ks_seed_encrypt(byte[] kbuf, byte[] mbuf)
			throws java.security.NoSuchProviderException,
			javax.crypto.BadPaddingException,
			javax.crypto.NoSuchPaddingException,
			java.security.InvalidAlgorithmParameterException,
			java.security.NoSuchAlgorithmException,
			java.security.InvalidKeyException,
			javax.crypto.IllegalBlockSizeException
	{
		byte tdata[] = new KSPaySeed(kbuf).cbc_encrypt(mbuf);
		return tdata;
	}

	public static byte[] ks_rsa_encrypt(byte[] sbuf)
			throws java.security.NoSuchProviderException,
			javax.crypto.NoSuchPaddingException,
			javax.crypto.BadPaddingException,
			java.security.NoSuchAlgorithmException,
			java.security.spec.InvalidKeySpecException,
			java.security.InvalidKeyException,
			javax.crypto.IllegalBlockSizeException
	{
		BigInteger modulus = new BigInteger(
				"d4846c2b8228dddfab9e614da2a324c1cc7b29d848cc005624d3a09667a2aab9073290bace6aa536ddceb3c47ddda78d9954da06c83aa65b939c5ec773a3787e71bec5a1c077bb446c06b393d2537967645d386b4b0b4ec21372fdc728c56693028c1c3915c1c4279793eb3dccefd6bf49b86cc7d88a47b0d44aba9e73750fcd",
				16);
		BigInteger publicExponent = new BigInteger(
				"0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010001",
				16);

		RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus,
				publicExponent);

		// JDK 1.4 이하에서는 BouncyCastleProvider를 사용(아래주석)
		// KeyFactory keyfactory = KeyFactory.getInstance("RSA",
		// CIPHER_PROVIDER);
		KeyFactory keyfactory = KeyFactory.getInstance("RSA");
		java.security.PublicKey publickey = keyfactory
				.generatePublic(pubKeySpec);

		// JDK 1.4 이하에서는 BouncyCastleProvider를 사용(아래주석)
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

		cipher.init(Cipher.ENCRYPT_MODE, publickey);// , fixedsecurerandom);

		byte[] rbuf = cipher.doFinal(sbuf);

		return rbuf;
	}
}

// //// 암호화 관련
// ////////////////////////////////////////////////////////////////////////////////////////////
class KSPaySeed
{
	private static final int SS0[] = { 0x2989a1a8, 0x5858184, 0x16c6d2d4,
			0x13c3d3d0, 0x14445054, 0x1d0d111c, 0x2c8ca0ac, 0x25052124,
			0x1d4d515c, 0x3434340, 0x18081018, 0x1e0e121c, 0x11415150,
			0x3cccf0fc, 0xacac2c8, 0x23436360, 0x28082028, 0x4444044,
			0x20002020, 0x1d8d919c, 0x20c0e0e0, 0x22c2e2e0, 0x8c8c0c8,
			0x17071314, 0x2585a1a4, 0xf8f838c, 0x3030300, 0x3b4b7378,
			0x3b8bb3b8, 0x13031310, 0x12c2d2d0, 0x2ecee2ec, 0x30407070,
			0xc8c808c, 0x3f0f333c, 0x2888a0a8, 0x32023230, 0x1dcdd1dc,
			0x36c6f2f4, 0x34447074, 0x2ccce0ec, 0x15859194, 0xb0b0308,
			0x17475354, 0x1c4c505c, 0x1b4b5358, 0x3d8db1bc, 0x1010100,
			0x24042024, 0x1c0c101c, 0x33437370, 0x18889098, 0x10001010,
			0xcccc0cc, 0x32c2f2f0, 0x19c9d1d8, 0x2c0c202c, 0x27c7e3e4,
			0x32427270, 0x3838380, 0x1b8b9398, 0x11c1d1d0, 0x6868284,
			0x9c9c1c8, 0x20406060, 0x10405050, 0x2383a3a0, 0x2bcbe3e8,
			0xd0d010c, 0x3686b2b4, 0x1e8e929c, 0xf4f434c, 0x3787b3b4,
			0x1a4a5258, 0x6c6c2c4, 0x38487078, 0x2686a2a4, 0x12021210,
			0x2f8fa3ac, 0x15c5d1d4, 0x21416160, 0x3c3c3c0, 0x3484b0b4,
			0x1414140, 0x12425250, 0x3d4d717c, 0xd8d818c, 0x8080008,
			0x1f0f131c, 0x19899198, 0, 0x19091118, 0x4040004, 0x13435350,
			0x37c7f3f4, 0x21c1e1e0, 0x3dcdf1fc, 0x36467274, 0x2f0f232c,
			0x27072324, 0x3080b0b0, 0xb8b8388, 0xe0e020c, 0x2b8ba3a8,
			0x2282a2a0, 0x2e4e626c, 0x13839390, 0xd4d414c, 0x29496168,
			0x3c4c707c, 0x9090108, 0xa0a0208, 0x3f8fb3bc, 0x2fcfe3ec,
			0x33c3f3f0, 0x5c5c1c4, 0x7878384, 0x14041014, 0x3ecef2fc,
			0x24446064, 0x1eced2dc, 0x2e0e222c, 0xb4b4348, 0x1a0a1218,
			0x6060204, 0x21012120, 0x2b4b6368, 0x26466264, 0x2020200,
			0x35c5f1f4, 0x12829290, 0xa8a8288, 0xc0c000c, 0x3383b3b0,
			0x3e4e727c, 0x10c0d0d0, 0x3a4a7278, 0x7474344, 0x16869294,
			0x25c5e1e4, 0x26062224, 0x808080, 0x2d8da1ac, 0x1fcfd3dc,
			0x2181a1a0, 0x30003030, 0x37073334, 0x2e8ea2ac, 0x36063234,
			0x15051114, 0x22022220, 0x38083038, 0x34c4f0f4, 0x2787a3a4,
			0x5454144, 0xc4c404c, 0x1818180, 0x29c9e1e8, 0x4848084, 0x17879394,
			0x35053134, 0xbcbc3c8, 0xecec2cc, 0x3c0c303c, 0x31417170,
			0x11011110, 0x7c7c3c4, 0x9898188, 0x35457174, 0x3bcbf3f8,
			0x1acad2d8, 0x38c8f0f8, 0x14849094, 0x19495158, 0x2828280,
			0x4c4c0c4, 0x3fcff3fc, 0x9494148, 0x39093138, 0x27476364, 0xc0c0c0,
			0xfcfc3cc, 0x17c7d3d4, 0x3888b0b8, 0xf0f030c, 0xe8e828c, 0x2424240,
			0x23032320, 0x11819190, 0x2c4c606c, 0x1bcbd3d8, 0x2484a0a4,
			0x34043034, 0x31c1f1f0, 0x8484048, 0x2c2c2c0, 0x2f4f636c,
			0x3d0d313c, 0x2d0d212c, 0x404040, 0x3e8eb2bc, 0x3e0e323c,
			0x3c8cb0bc, 0x1c1c1c0, 0x2a8aa2a8, 0x3a8ab2b8, 0xe4e424c,
			0x15455154, 0x3b0b3338, 0x1cccd0dc, 0x28486068, 0x3f4f737c,
			0x1c8c909c, 0x18c8d0d8, 0xa4a4248, 0x16465254, 0x37477374,
			0x2080a0a0, 0x2dcde1ec, 0x6464244, 0x3585b1b4, 0x2b0b2328,
			0x25456164, 0x3acaf2f8, 0x23c3e3e0, 0x3989b1b8, 0x3181b1b0,
			0x1f8f939c, 0x1e4e525c, 0x39c9f1f8, 0x26c6e2e4, 0x3282b2b0,
			0x31013130, 0x2acae2e8, 0x2d4d616c, 0x1f4f535c, 0x24c4e0e4,
			0x30c0f0f0, 0xdcdc1cc, 0x8888088, 0x16061214, 0x3a0a3238,
			0x18485058, 0x14c4d0d4, 0x22426260, 0x29092128, 0x7070304,
			0x33033330, 0x28c8e0e8, 0x1b0b1318, 0x5050104, 0x39497178,
			0x10809090, 0x2a4a6268, 0x2a0a2228, 0x1a8a9298 };

	private static final int SS1[] = { 0x38380830, 0xe828c8e0, 0x2c2d0d21,
			0xa42686a2, 0xcc0fcfc3, 0xdc1eced2, 0xb03383b3, 0xb83888b0,
			0xac2f8fa3, 0x60204060, 0x54154551, 0xc407c7c3, 0x44044440,
			0x6c2f4f63, 0x682b4b63, 0x581b4b53, 0xc003c3c3, 0x60224262,
			0x30330333, 0xb43585b1, 0x28290921, 0xa02080a0, 0xe022c2e2,
			0xa42787a3, 0xd013c3d3, 0x90118191, 0x10110111, 0x4060602,
			0x1c1c0c10, 0xbc3c8cb0, 0x34360632, 0x480b4b43, 0xec2fcfe3,
			0x88088880, 0x6c2c4c60, 0xa82888a0, 0x14170713, 0xc404c4c0,
			0x14160612, 0xf434c4f0, 0xc002c2c2, 0x44054541, 0xe021c1e1,
			0xd416c6d2, 0x3c3f0f33, 0x3c3d0d31, 0x8c0e8e82, 0x98188890,
			0x28280820, 0x4c0e4e42, 0xf436c6f2, 0x3c3e0e32, 0xa42585a1,
			0xf839c9f1, 0xc0d0d01, 0xdc1fcfd3, 0xd818c8d0, 0x282b0b23,
			0x64264662, 0x783a4a72, 0x24270723, 0x2c2f0f23, 0xf031c1f1,
			0x70324272, 0x40024242, 0xd414c4d0, 0x40014141, 0xc000c0c0,
			0x70334373, 0x64274763, 0xac2c8ca0, 0x880b8b83, 0xf437c7f3,
			0xac2d8da1, 0x80008080, 0x1c1f0f13, 0xc80acac2, 0x2c2c0c20,
			0xa82a8aa2, 0x34340430, 0xd012c2d2, 0x80b0b03, 0xec2ecee2,
			0xe829c9e1, 0x5c1d4d51, 0x94148490, 0x18180810, 0xf838c8f0,
			0x54174753, 0xac2e8ea2, 0x8080800, 0xc405c5c1, 0x10130313,
			0xcc0dcdc1, 0x84068682, 0xb83989b1, 0xfc3fcff3, 0x7c3d4d71,
			0xc001c1c1, 0x30310131, 0xf435c5f1, 0x880a8a82, 0x682a4a62,
			0xb03181b1, 0xd011c1d1, 0x20200020, 0xd417c7d3, 0x20202,
			0x20220222, 0x4040400, 0x68284860, 0x70314171, 0x4070703,
			0xd81bcbd3, 0x9c1d8d91, 0x98198991, 0x60214161, 0xbc3e8eb2,
			0xe426c6e2, 0x58194951, 0xdc1dcdd1, 0x50114151, 0x90108090,
			0xdc1cccd0, 0x981a8a92, 0xa02383a3, 0xa82b8ba3, 0xd010c0d0,
			0x80018181, 0xc0f0f03, 0x44074743, 0x181a0a12, 0xe023c3e3,
			0xec2ccce0, 0x8c0d8d81, 0xbc3f8fb3, 0x94168692, 0x783b4b73,
			0x5c1c4c50, 0xa02282a2, 0xa02181a1, 0x60234363, 0x20230323,
			0x4c0d4d41, 0xc808c8c0, 0x9c1e8e92, 0x9c1c8c90, 0x383a0a32,
			0xc0c0c00, 0x2c2e0e22, 0xb83a8ab2, 0x6c2e4e62, 0x9c1f8f93,
			0x581a4a52, 0xf032c2f2, 0x90128292, 0xf033c3f3, 0x48094941,
			0x78384870, 0xcc0cccc0, 0x14150511, 0xf83bcbf3, 0x70304070,
			0x74354571, 0x7c3f4f73, 0x34350531, 0x10100010, 0x30303,
			0x64244460, 0x6c2d4d61, 0xc406c6c2, 0x74344470, 0xd415c5d1,
			0xb43484b0, 0xe82acae2, 0x8090901, 0x74364672, 0x18190911,
			0xfc3ecef2, 0x40004040, 0x10120212, 0xe020c0e0, 0xbc3d8db1,
			0x4050501, 0xf83acaf2, 0x10101, 0xf030c0f0, 0x282a0a22, 0x5c1e4e52,
			0xa82989a1, 0x54164652, 0x40034343, 0x84058581, 0x14140410,
			0x88098981, 0x981b8b93, 0xb03080b0, 0xe425c5e1, 0x48084840,
			0x78394971, 0x94178793, 0xfc3cccf0, 0x1c1e0e12, 0x80028282,
			0x20210121, 0x8c0c8c80, 0x181b0b13, 0x5c1f4f53, 0x74374773,
			0x54144450, 0xb03282b2, 0x1c1d0d11, 0x24250521, 0x4c0f4f43, 0,
			0x44064642, 0xec2dcde1, 0x58184850, 0x50124252, 0xe82bcbe3,
			0x7c3e4e72, 0xd81acad2, 0xc809c9c1, 0xfc3dcdf1, 0x30300030,
			0x94158591, 0x64254561, 0x3c3c0c30, 0xb43686b2, 0xe424c4e0,
			0xb83b8bb3, 0x7c3c4c70, 0xc0e0e02, 0x50104050, 0x38390931,
			0x24260622, 0x30320232, 0x84048480, 0x68294961, 0x90138393,
			0x34370733, 0xe427c7e3, 0x24240420, 0xa42484a0, 0xc80bcbc3,
			0x50134353, 0x80a0a02, 0x84078783, 0xd819c9d1, 0x4c0c4c40,
			0x80038383, 0x8c0f8f83, 0xcc0ecec2, 0x383b0b33, 0x480a4a42,
			0xb43787b3 };

	private static final int SS2[] = { 0xa1a82989, 0x81840585, 0xd2d416c6,
			0xd3d013c3, 0x50541444, 0x111c1d0d, 0xa0ac2c8c, 0x21242505,
			0x515c1d4d, 0x43400343, 0x10181808, 0x121c1e0e, 0x51501141,
			0xf0fc3ccc, 0xc2c80aca, 0x63602343, 0x20282808, 0x40440444,
			0x20202000, 0x919c1d8d, 0xe0e020c0, 0xe2e022c2, 0xc0c808c8,
			0x13141707, 0xa1a42585, 0x838c0f8f, 0x3000303, 0x73783b4b,
			0xb3b83b8b, 0x13101303, 0xd2d012c2, 0xe2ec2ece, 0x70703040,
			0x808c0c8c, 0x333c3f0f, 0xa0a82888, 0x32303202, 0xd1dc1dcd,
			0xf2f436c6, 0x70743444, 0xe0ec2ccc, 0x91941585, 0x3080b0b,
			0x53541747, 0x505c1c4c, 0x53581b4b, 0xb1bc3d8d, 0x1000101,
			0x20242404, 0x101c1c0c, 0x73703343, 0x90981888, 0x10101000,
			0xc0cc0ccc, 0xf2f032c2, 0xd1d819c9, 0x202c2c0c, 0xe3e427c7,
			0x72703242, 0x83800383, 0x93981b8b, 0xd1d011c1, 0x82840686,
			0xc1c809c9, 0x60602040, 0x50501040, 0xa3a02383, 0xe3e82bcb,
			0x10c0d0d, 0xb2b43686, 0x929c1e8e, 0x434c0f4f, 0xb3b43787,
			0x52581a4a, 0xc2c406c6, 0x70783848, 0xa2a42686, 0x12101202,
			0xa3ac2f8f, 0xd1d415c5, 0x61602141, 0xc3c003c3, 0xb0b43484,
			0x41400141, 0x52501242, 0x717c3d4d, 0x818c0d8d, 0x80808,
			0x131c1f0f, 0x91981989, 0, 0x11181909, 0x40404, 0x53501343,
			0xf3f437c7, 0xe1e021c1, 0xf1fc3dcd, 0x72743646, 0x232c2f0f,
			0x23242707, 0xb0b03080, 0x83880b8b, 0x20c0e0e, 0xa3a82b8b,
			0xa2a02282, 0x626c2e4e, 0x93901383, 0x414c0d4d, 0x61682949,
			0x707c3c4c, 0x1080909, 0x2080a0a, 0xb3bc3f8f, 0xe3ec2fcf,
			0xf3f033c3, 0xc1c405c5, 0x83840787, 0x10141404, 0xf2fc3ece,
			0x60642444, 0xd2dc1ece, 0x222c2e0e, 0x43480b4b, 0x12181a0a,
			0x2040606, 0x21202101, 0x63682b4b, 0x62642646, 0x2000202,
			0xf1f435c5, 0x92901282, 0x82880a8a, 0xc0c0c, 0xb3b03383,
			0x727c3e4e, 0xd0d010c0, 0x72783a4a, 0x43440747, 0x92941686,
			0xe1e425c5, 0x22242606, 0x80800080, 0xa1ac2d8d, 0xd3dc1fcf,
			0xa1a02181, 0x30303000, 0x33343707, 0xa2ac2e8e, 0x32343606,
			0x11141505, 0x22202202, 0x30383808, 0xf0f434c4, 0xa3a42787,
			0x41440545, 0x404c0c4c, 0x81800181, 0xe1e829c9, 0x80840484,
			0x93941787, 0x31343505, 0xc3c80bcb, 0xc2cc0ece, 0x303c3c0c,
			0x71703141, 0x11101101, 0xc3c407c7, 0x81880989, 0x71743545,
			0xf3f83bcb, 0xd2d81aca, 0xf0f838c8, 0x90941484, 0x51581949,
			0x82800282, 0xc0c404c4, 0xf3fc3fcf, 0x41480949, 0x31383909,
			0x63642747, 0xc0c000c0, 0xc3cc0fcf, 0xd3d417c7, 0xb0b83888,
			0x30c0f0f, 0x828c0e8e, 0x42400242, 0x23202303, 0x91901181,
			0x606c2c4c, 0xd3d81bcb, 0xa0a42484, 0x30343404, 0xf1f031c1,
			0x40480848, 0xc2c002c2, 0x636c2f4f, 0x313c3d0d, 0x212c2d0d,
			0x40400040, 0xb2bc3e8e, 0x323c3e0e, 0xb0bc3c8c, 0xc1c001c1,
			0xa2a82a8a, 0xb2b83a8a, 0x424c0e4e, 0x51541545, 0x33383b0b,
			0xd0dc1ccc, 0x60682848, 0x737c3f4f, 0x909c1c8c, 0xd0d818c8,
			0x42480a4a, 0x52541646, 0x73743747, 0xa0a02080, 0xe1ec2dcd,
			0x42440646, 0xb1b43585, 0x23282b0b, 0x61642545, 0xf2f83aca,
			0xe3e023c3, 0xb1b83989, 0xb1b03181, 0x939c1f8f, 0x525c1e4e,
			0xf1f839c9, 0xe2e426c6, 0xb2b03282, 0x31303101, 0xe2e82aca,
			0x616c2d4d, 0x535c1f4f, 0xe0e424c4, 0xf0f030c0, 0xc1cc0dcd,
			0x80880888, 0x12141606, 0x32383a0a, 0x50581848, 0xd0d414c4,
			0x62602242, 0x21282909, 0x3040707, 0x33303303, 0xe0e828c8,
			0x13181b0b, 0x1040505, 0x71783949, 0x90901080, 0x62682a4a,
			0x22282a0a, 0x92981a8a };

	private static final int SS3[] = { 0x8303838, 0xc8e0e828, 0xd212c2d,
			0x86a2a426, 0xcfc3cc0f, 0xced2dc1e, 0x83b3b033, 0x88b0b838,
			0x8fa3ac2f, 0x40606020, 0x45515415, 0xc7c3c407, 0x44404404,
			0x4f636c2f, 0x4b63682b, 0x4b53581b, 0xc3c3c003, 0x42626022,
			0x3333033, 0x85b1b435, 0x9212829, 0x80a0a020, 0xc2e2e022,
			0x87a3a427, 0xc3d3d013, 0x81919011, 0x1111011, 0x6020406,
			0xc101c1c, 0x8cb0bc3c, 0x6323436, 0x4b43480b, 0xcfe3ec2f,
			0x88808808, 0x4c606c2c, 0x88a0a828, 0x7131417, 0xc4c0c404,
			0x6121416, 0xc4f0f434, 0xc2c2c002, 0x45414405, 0xc1e1e021,
			0xc6d2d416, 0xf333c3f, 0xd313c3d, 0x8e828c0e, 0x88909818,
			0x8202828, 0x4e424c0e, 0xc6f2f436, 0xe323c3e, 0x85a1a425,
			0xc9f1f839, 0xd010c0d, 0xcfd3dc1f, 0xc8d0d818, 0xb23282b,
			0x46626426, 0x4a72783a, 0x7232427, 0xf232c2f, 0xc1f1f031,
			0x42727032, 0x42424002, 0xc4d0d414, 0x41414001, 0xc0c0c000,
			0x43737033, 0x47636427, 0x8ca0ac2c, 0x8b83880b, 0xc7f3f437,
			0x8da1ac2d, 0x80808000, 0xf131c1f, 0xcac2c80a, 0xc202c2c,
			0x8aa2a82a, 0x4303434, 0xc2d2d012, 0xb03080b, 0xcee2ec2e,
			0xc9e1e829, 0x4d515c1d, 0x84909414, 0x8101818, 0xc8f0f838,
			0x47535417, 0x8ea2ac2e, 0x8000808, 0xc5c1c405, 0x3131013,
			0xcdc1cc0d, 0x86828406, 0x89b1b839, 0xcff3fc3f, 0x4d717c3d,
			0xc1c1c001, 0x1313031, 0xc5f1f435, 0x8a82880a, 0x4a62682a,
			0x81b1b031, 0xc1d1d011, 0x202020, 0xc7d3d417, 0x2020002, 0x2222022,
			0x4000404, 0x48606828, 0x41717031, 0x7030407, 0xcbd3d81b,
			0x8d919c1d, 0x89919819, 0x41616021, 0x8eb2bc3e, 0xc6e2e426,
			0x49515819, 0xcdd1dc1d, 0x41515011, 0x80909010, 0xccd0dc1c,
			0x8a92981a, 0x83a3a023, 0x8ba3a82b, 0xc0d0d010, 0x81818001,
			0xf030c0f, 0x47434407, 0xa12181a, 0xc3e3e023, 0xcce0ec2c,
			0x8d818c0d, 0x8fb3bc3f, 0x86929416, 0x4b73783b, 0x4c505c1c,
			0x82a2a022, 0x81a1a021, 0x43636023, 0x3232023, 0x4d414c0d,
			0xc8c0c808, 0x8e929c1e, 0x8c909c1c, 0xa32383a, 0xc000c0c,
			0xe222c2e, 0x8ab2b83a, 0x4e626c2e, 0x8f939c1f, 0x4a52581a,
			0xc2f2f032, 0x82929012, 0xc3f3f033, 0x49414809, 0x48707838,
			0xccc0cc0c, 0x5111415, 0xcbf3f83b, 0x40707030, 0x45717435,
			0x4f737c3f, 0x5313435, 0x101010, 0x3030003, 0x44606424, 0x4d616c2d,
			0xc6c2c406, 0x44707434, 0xc5d1d415, 0x84b0b434, 0xcae2e82a,
			0x9010809, 0x46727436, 0x9111819, 0xcef2fc3e, 0x40404000,
			0x2121012, 0xc0e0e020, 0x8db1bc3d, 0x5010405, 0xcaf2f83a,
			0x1010001, 0xc0f0f030, 0xa22282a, 0x4e525c1e, 0x89a1a829,
			0x46525416, 0x43434003, 0x85818405, 0x4101414, 0x89818809,
			0x8b93981b, 0x80b0b030, 0xc5e1e425, 0x48404808, 0x49717839,
			0x87939417, 0xccf0fc3c, 0xe121c1e, 0x82828002, 0x1212021,
			0x8c808c0c, 0xb13181b, 0x4f535c1f, 0x47737437, 0x44505414,
			0x82b2b032, 0xd111c1d, 0x5212425, 0x4f434c0f, 0, 0x46424406,
			0xcde1ec2d, 0x48505818, 0x42525012, 0xcbe3e82b, 0x4e727c3e,
			0xcad2d81a, 0xc9c1c809, 0xcdf1fc3d, 0x303030, 0x85919415,
			0x45616425, 0xc303c3c, 0x86b2b436, 0xc4e0e424, 0x8bb3b83b,
			0x4c707c3c, 0xe020c0e, 0x40505010, 0x9313839, 0x6222426, 0x2323032,
			0x84808404, 0x49616829, 0x83939013, 0x7333437, 0xc7e3e427,
			0x4202424, 0x84a0a424, 0xcbc3c80b, 0x43535013, 0xa02080a,
			0x87838407, 0xc9d1d819, 0x4c404c0c, 0x83838003, 0x8f838c0f,
			0xcec2cc0e, 0xb33383b, 0x4a42480a, 0x87b3b437 };

	private static final int KC[] = { 0x9e3779b9, 0x3c6ef373, 0x78dde6e6,
			0xf1bbcdcc, 0xe3779b99, 0xc6ef3733, 0x8dde6e67, 0x1bbcdccf,
			0x3779b99e, 0x6ef3733c, 0xdde6e678, 0xbbcdccf1, 0x779b99e3,
			0xef3733c6, 0xde6e678d, 0xbcdccf1b };

	private static final boolean LITTLE = true;
	private static final boolean ENDIAN = LITTLE;
	private static final int NoRounds = 16;

	/** Seed Block Size (16byte) */
	private static final int SeedBlockSize = 16;

	private int GetB0(int A)
	{
		return 0xff & A;
	}

	private int GetB1(int A)
	{
		return 0xff & A >>> 8;
	}

	private int GetB2(int A)
	{
		return 0xff & A >>> 16;
	}

	private int GetB3(int A)
	{
		return 0xff & A >>> 24;
	}

	private void EndianChange(int dws[])
	{
		dws[0] = dws[0] >>> 24 | dws[0] << 24 | dws[0] << 8 & 0xff0000
				| dws[0] >>> 8 & 0xff00;
	}

	private int EndianChange(int dws)
	{
		return dws >>> 24 | dws << 24 | dws << 8 & 0xff0000 | dws >>> 8
				& 0xff00;
	}

	private void SeedRound(int L0[], int L1[], int R0[], int R1[], int K[])
	{
		long T00 = 0L;
		long T11 = 0L;
		int T0 = R0[0] ^ K[0];
		int T1 = R1[0] ^ K[1];
		T1 ^= T0;
		T00 = T0 >= 0 ? T0 : (T0 & 0x7fffffff) | 0xffffffff80000000L;
		T1 = SS0[GetB0(T1)] ^ SS1[GetB1(T1)] ^ SS2[GetB2(T1)] ^ SS3[GetB3(T1)];
		T11 = T1 >= 0 ? T1 : (T1 & 0x7fffffff) | 0xffffffff80000000L;
		T00 += T11;
		T0 = SS0[GetB0((int) T00)] ^ SS1[GetB1((int) T00)]
				^ SS2[GetB2((int) T00)] ^ SS3[GetB3((int) T00)];
		T00 = T0 >= 0 ? T0 : (T0 & 0x7fffffff) | 0xffffffff80000000L;
		T11 += T00;
		T1 = SS0[GetB0((int) T11)] ^ SS1[GetB1((int) T11)]
				^ SS2[GetB2((int) T11)] ^ SS3[GetB3((int) T11)];
		T11 = T1 >= 0 ? T1 : (T1 & 0x7fffffff) | 0xffffffff80000000L;
		T00 += T11;
		L0[0] ^= (int) T00;
		L1[0] ^= (int) T11;
	}

	private void SeedEncrypt(byte pbData[], int pidx, int pdwRoundKey[],
			byte outData[], int oidx)
	{
		int L0[] = new int[1];
		int L1[] = new int[1];
		int R0[] = new int[1];
		int R1[] = new int[1];
		L0[0] = 0;
		L1[0] = 0;
		R0[0] = 0;
		R1[0] = 0;
		int K[] = new int[2];
		int nCount = 0;
		L0[0] = pbData[pidx + 0] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 1] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 2] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 3] & 0xff;
		L1[0] = pbData[pidx + 4] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 5] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 6] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 7] & 0xff;
		R0[0] = pbData[pidx + 8] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 9] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 10] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 11] & 0xff;
		R1[0] = pbData[pidx + 12] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 13] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 14] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 15] & 0xff;
		if (!ENDIAN)
		{
			EndianChange(L0);
			EndianChange(L1);
			EndianChange(R0);
			EndianChange(R1);
		}
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(L0, L1, R0, R1, K);
		K[0] = pdwRoundKey[nCount++];
		K[1] = pdwRoundKey[nCount++];
		SeedRound(R0, R1, L0, L1, K);

		if (NoRounds == 16)
		{
			K[0] = pdwRoundKey[nCount++];
			K[1] = pdwRoundKey[nCount++];
			SeedRound(L0, L1, R0, R1, K);
			K[0] = pdwRoundKey[nCount++];
			K[1] = pdwRoundKey[nCount++];
			SeedRound(R0, R1, L0, L1, K);
			K[0] = pdwRoundKey[nCount++];
			K[1] = pdwRoundKey[nCount++];
			SeedRound(L0, L1, R0, R1, K);
			K[0] = pdwRoundKey[nCount++];
			K[1] = pdwRoundKey[nCount++];
			SeedRound(R0, R1, L0, L1, K);
		}

		if (!ENDIAN)
		{
			EndianChange(L0);
			EndianChange(L1);
			EndianChange(R0);
			EndianChange(R1);
		}

		for (int i = 0; i < 4; i++)
		{
			outData[oidx + i] = (byte) (R0[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 4 + i] = (byte) (R1[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 8 + i] = (byte) (L0[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 12 + i] = (byte) (L1[0] >>> 8 * (3 - i) & 0xff);
		}

	}

	private void SeedDecrypt(byte pbData[], int pidx, int pdwRoundKey[],
			byte outData[], int oidx)
	{
		int L0[] = new int[1];
		int L1[] = new int[1];
		int R0[] = new int[1];
		int R1[] = new int[1];
		int K[] = new int[2];
		L0[0] = 0;
		L1[0] = 0;
		R0[0] = 0;
		R1[0] = 0;
		int nCount = 31;
		L0[0] = pbData[pidx + 0] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 1] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 2] & 0xff;
		L0[0] = L0[0] << 8 ^ pbData[pidx + 3] & 0xff;
		L1[0] = pbData[pidx + 4] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 5] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 6] & 0xff;
		L1[0] = L1[0] << 8 ^ pbData[pidx + 7] & 0xff;
		R0[0] = pbData[pidx + 8] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 9] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 10] & 0xff;
		R0[0] = R0[0] << 8 ^ pbData[pidx + 11] & 0xff;
		R1[0] = pbData[pidx + 12] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 13] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 14] & 0xff;
		R1[0] = R1[0] << 8 ^ pbData[pidx + 15] & 0xff;

		if (!ENDIAN)
		{
			EndianChange(L0);
			EndianChange(L1);
			EndianChange(R0);
			EndianChange(R1);
		}

		if (NoRounds == 16)
		{
			K[1] = pdwRoundKey[nCount--];
			K[0] = pdwRoundKey[nCount--];
			SeedRound(L0, L1, R0, R1, K);
			K[1] = pdwRoundKey[nCount--];
			K[0] = pdwRoundKey[nCount--];
			SeedRound(R0, R1, L0, L1, K);
			K[1] = pdwRoundKey[nCount--];
			K[0] = pdwRoundKey[nCount--];
			SeedRound(L0, L1, R0, R1, K);
			K[1] = pdwRoundKey[nCount--];
			K[0] = pdwRoundKey[nCount--];
			SeedRound(R0, R1, L0, L1, K);
		}

		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(R0, R1, L0, L1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(R0, R1, L0, L1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(R0, R1, L0, L1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(R0, R1, L0, L1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(R0, R1, L0, L1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount--];
		SeedRound(L0, L1, R0, R1, K);
		K[1] = pdwRoundKey[nCount--];
		K[0] = pdwRoundKey[nCount];
		SeedRound(R0, R1, L0, L1, K);

		if (!ENDIAN)
		{
			EndianChange(L0);
			EndianChange(L1);
			EndianChange(R0);
			EndianChange(R1);
		}

		for (int i = 0; i < 4; i++)
		{
			outData[oidx + i] = (byte) (R0[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 4 + i] = (byte) (R1[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 8 + i] = (byte) (L0[0] >>> 8 * (3 - i) & 0xff);
			outData[oidx + 12 + i] = (byte) (L1[0] >>> 8 * (3 - i) & 0xff);
		}

	}

	private void EncRoundKeyUpdate0(int K[], int A[], int B[], int C[],
			int D[], int Z)
	{

		int T0 = A[0];
		A[0] = A[0] >>> 8 ^ B[0] << 24;
		B[0] = B[0] >>> 8 ^ T0 << 24;
		int T00 = (A[0] + C[0]) - KC[Z];
		int T11 = (B[0] + KC[Z]) - D[0];
		K[0] = SS0[GetB0(T00)] ^ SS1[GetB1(T00)] ^ SS2[GetB2(T00)]
				^ SS3[GetB3(T00)];
		K[1] = SS0[GetB0(T11)] ^ SS1[GetB1(T11)] ^ SS2[GetB2(T11)]
				^ SS3[GetB3(T11)];
	}

	private void EncRoundKeyUpdate1(int K[], int A[], int B[], int C[],
			int D[], int Z)
	{

		int T0 = C[0];
		C[0] = C[0] << 8 ^ D[0] >>> 24;
		D[0] = D[0] << 8 ^ T0 >>> 24;
		int T00 = (A[0] + C[0]) - KC[Z];
		int T11 = (B[0] + KC[Z]) - D[0];
		K[0] = SS0[GetB0(T00)] ^ SS1[GetB1(T00)] ^ SS2[GetB2(T00)]
				^ SS3[GetB3(T00)];
		K[1] = SS0[GetB0(T11)] ^ SS1[GetB1(T11)] ^ SS2[GetB2(T11)]
				^ SS3[GetB3(T11)];
	}

	private void SeedEncRoundKey(int pdwRoundKey[], byte pbUserKey[])
	{
		int A[] = new int[1];
		int B[] = new int[1];
		int C[] = new int[1];
		int D[] = new int[1];
		int K[] = new int[2];
		int nCount = 2;
		A[0] = pbUserKey[0] & 0xff;
		A[0] = A[0] << 8 ^ pbUserKey[1] & 0xff;
		A[0] = A[0] << 8 ^ pbUserKey[2] & 0xff;
		A[0] = A[0] << 8 ^ pbUserKey[3] & 0xff;
		B[0] = pbUserKey[4] & 0xff;
		B[0] = B[0] << 8 ^ pbUserKey[5] & 0xff;
		B[0] = B[0] << 8 ^ pbUserKey[6] & 0xff;
		B[0] = B[0] << 8 ^ pbUserKey[7] & 0xff;
		C[0] = pbUserKey[8] & 0xff;
		C[0] = C[0] << 8 ^ pbUserKey[9] & 0xff;
		C[0] = C[0] << 8 ^ pbUserKey[10] & 0xff;
		C[0] = C[0] << 8 ^ pbUserKey[11] & 0xff;
		D[0] = pbUserKey[12] & 0xff;
		D[0] = D[0] << 8 ^ pbUserKey[13] & 0xff;
		D[0] = D[0] << 8 ^ pbUserKey[14] & 0xff;
		D[0] = D[0] << 8 ^ pbUserKey[15] & 0xff;
		if (!ENDIAN)
		{
			A[0] = EndianChange(A[0]);
			B[0] = EndianChange(B[0]);
			C[0] = EndianChange(C[0]);
			D[0] = EndianChange(D[0]);
		}
		int T0 = (A[0] + C[0]) - KC[0];
		int T1 = (B[0] - D[0]) + KC[0];
		pdwRoundKey[0] = SS0[GetB0(T0)] ^ SS1[GetB1(T0)] ^ SS2[GetB2(T0)]
				^ SS3[GetB3(T0)];
		pdwRoundKey[1] = SS0[GetB0(T1)] ^ SS1[GetB1(T1)] ^ SS2[GetB2(T1)]
				^ SS3[GetB3(T1)];
		EncRoundKeyUpdate0(K, A, B, C, D, 1);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate1(K, A, B, C, D, 2);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate0(K, A, B, C, D, 3);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate1(K, A, B, C, D, 4);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate0(K, A, B, C, D, 5);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate1(K, A, B, C, D, 6);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate0(K, A, B, C, D, 7);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate1(K, A, B, C, D, 8);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate0(K, A, B, C, D, 9);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate1(K, A, B, C, D, 10);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		EncRoundKeyUpdate0(K, A, B, C, D, 11);
		pdwRoundKey[nCount++] = K[0];
		pdwRoundKey[nCount++] = K[1];
		if (NoRounds == 16)
		{
			EncRoundKeyUpdate1(K, A, B, C, D, 12);
			pdwRoundKey[nCount++] = K[0];
			pdwRoundKey[nCount++] = K[1];
			EncRoundKeyUpdate0(K, A, B, C, D, 13);
			pdwRoundKey[nCount++] = K[0];
			pdwRoundKey[nCount++] = K[1];
			EncRoundKeyUpdate1(K, A, B, C, D, 14);
			pdwRoundKey[nCount++] = K[0];
			pdwRoundKey[nCount++] = K[1];
			EncRoundKeyUpdate0(K, A, B, C, D, 15);
			pdwRoundKey[nCount++] = K[0];
			pdwRoundKey[nCount++] = K[1];
		}
	}

	public KSPaySeed(String pbUserKeyStr)
	{
		SeedEncRoundKey(sRoundKey, KSStrByteUtil.s2b(pbUserKeyStr));
	}

	public KSPaySeed(byte pbUserKey[])
	{
		SeedEncRoundKey(sRoundKey, pbUserKey);
	}

	private int sRoundKey[] = new int[32];
	private byte iv[] = new byte[16];

	static final int NULL_PADDING = 0, SPACE_PADDING = 1, PKCS5_PADDING = 2;

	byte[] addPadding(byte[] sbuf, int pMode, int bSize)
	{
		int plen = bSize - sbuf.length % bSize;
		byte pchar = (byte) 0x00;

		if (plen == bSize && pMode != PKCS5_PADDING)
			plen = 0;

		byte[] rbuf = new byte[sbuf.length + plen];
		System.arraycopy(sbuf, 0, rbuf, 0, sbuf.length);

		if (NULL_PADDING == pMode || 0 == plen)
			return rbuf;

		if (SPACE_PADDING == pMode)
			pchar = (byte) 0x20;
		else if (PKCS5_PADDING == pMode)
			pchar = (byte) plen;
		else
			throw new IllegalArgumentException("Padding(" + pMode
					+ ") - Not Implemented!!");

		java.util.Arrays.fill(rbuf, sbuf.length, rbuf.length, pchar);
		return rbuf;
	}

	byte[] removePadding(byte[] sbuf, int pMode, int bSize)
	{
		byte pchar = (byte) 0x00;
		int rlen = sbuf.length;

		if (NULL_PADDING == pMode)
			pchar = (byte) 0x00;
		else if (SPACE_PADDING == pMode)
			pchar = (byte) 0x20;
		else if (PKCS5_PADDING == pMode)
			pchar = sbuf[sbuf.length - 1];
		else
			throw new IllegalArgumentException("Padding(" + pMode
					+ ") - Not Implemented!!");

		if (PKCS5_PADDING == pMode)
		{
			rlen = sbuf.length - pchar;
		} else
		{
			for (int i = 0; i < bSize; i++)
			{
				if (pchar != sbuf[sbuf.length - 1 - i])
				{
					rlen = sbuf.length - i;
					break;
				}
			}
		}

		byte[] rbuf = new byte[rlen];
		System.arraycopy(sbuf, 0, rbuf, 0, rbuf.length);

		return rbuf;
	}

	public byte[] cbc_encrypt(byte[] sbuf)
	{

		// System.out.println("cbc_encrypt 0.sRoundKey ("+sRoundKey[0]+""+sRoundKey[1]+":"+sRoundKey[2]+":"+sRoundKey[3]+":"+sRoundKey[4]+":"+sRoundKey[5]+":"+sRoundKey[6]+":"+sRoundKey[7]+":"+sRoundKey[6]+":"+sRoundKey[9]+":"+sRoundKey[10]+":"+sRoundKey[11]+":"+sRoundKey[12]+":"+sRoundKey[13]+":"+sRoundKey[14]+":"+sRoundKey[15]+":"+sRoundKey[16]+":"+sRoundKey[17]+":"+sRoundKey[18]+":"+sRoundKey[19]+":"+sRoundKey[20]+":"+sRoundKey[21]+":"+sRoundKey[22]+":"+sRoundKey[23]+":"+sRoundKey[24]+":"+sRoundKey[25]+":"+sRoundKey[26]+":"+sRoundKey[27]+":"+sRoundKey[28]+":"+sRoundKey[29]+":"+sRoundKey[30]+":"+sRoundKey[31]+")");
		// System.out.println("cbc_encrypt 1.패딩전 ("+sbuf.length+":"+KSPaySeed.hex_encode(sbuf)+")");

		byte[] inData = addPadding(sbuf, PKCS5_PADDING, SeedBlockSize);

		// System.out.println("cbc_encrypt 2.패딩후 ("+inData.length+":"+KSPaySeed.hex_encode(inData)+")");

		byte[] encData = new byte[inData.length];

		byte[] xInput = (byte[]) iv.clone();
		byte[] xOutput = new byte[SeedBlockSize];

		for (int j = 0; j < inData.length; j += SeedBlockSize)
		{
			for (int k = 0; k < xOutput.length; k++)
			{
				xOutput[k] = (byte) (xInput[k] ^ inData[j + k]);
			}
			// System.out.println("cbc_encrypt 3."+j+" "+KSPaySeed.hex_encode(xOutput)+" = "+KSPaySeed.hex_encode(xInput)+" ^ "+KSPaySeed.hex_encode(inData)+" ");

			// SeedEncrypt(inData,j, sRoundKey, encData,j);
			SeedEncrypt(xOutput, 0, sRoundKey, encData, j);

			// System.out.println("cbc_encrypt 4.패딩후 ("+KSPaySeed.hex_encode(xOutput)+")");

			System.arraycopy(encData, j, xInput, 0, xInput.length);
		}

		return encData;
	}

	public byte[] cbc_decrypt(byte[] encData)
	{
		return cbc_decrypt(encData, true);
	}

	public byte[] cbc_decrypt(byte[] encData, boolean rmPadding)
	{
		byte[] decData = new byte[encData.length];

		byte[] xInput = (byte[]) iv.clone();
		byte[] xOutput = new byte[SeedBlockSize];

		for (int j = 0; j < encData.length; j += SeedBlockSize)
		{
			SeedDecrypt(encData, j, sRoundKey, xOutput, 0);// decData,j);

			for (int k = 0; k < xOutput.length; k++)
			{
				decData[j + k] = (byte) (xInput[k] ^ xOutput[k]);
			}
			System.arraycopy(encData, j, xInput, 0, xInput.length);
		}
		return (rmPadding) ? removePadding(decData, PKCS5_PADDING,
				SeedBlockSize) : decData;
	}

}

class KSMyException extends Exception
{
	private String except_cont;

	private KSMyException()
	{

	}

	public KSMyException(String msg)
	{
		except_cont = new String(msg);
	}

	public String toString()
	{
		return except_cont;
	}

	public void printStackTrace()
	{
		System.out.println(except_cont);
	}
}

class KSStrByteUtil
{
	/* C의 memset 구현 */
	public static void initBytes(byte[] buf, byte c)
	{
		for (int i = 0; i < buf.length; i++)
		{
			buf[i] = c;
		}
	}

	public static String hex_encode(byte sbuf[])
	{
		byte b0, b1;
		byte rbuf[] = new byte[sbuf.length * 2];

		for (int i = 0; i < sbuf.length; i++)
		{
			b0 = (byte) (sbuf[i] >> 4 & 0xf);
			rbuf[2 * i] = (byte) (b0 >= 10 ? (b0 - 10) + 65 : b0 + 48);
			b1 = (byte) (sbuf[i] & 0xf);
			rbuf[2 * i + 1] = (byte) (b1 >= 10 ? (b1 - 10) + 65 : b1 + 48);
		}

		return b2s(rbuf);
	}

	public static byte[] hex_decode(String sStr)
	{
		if (sStr.length() != sStr.length() / 2 * 2)
			return null;

		byte[] oData = new byte[sStr.length() / 2];

		for (int i = 0, j = 0; i < sStr.length() - 1; i += 2, j++)
		{
			oData[j] = (byte) (0xff & Integer.parseInt(
					sStr.substring(i, i + 2), 16));
		}

		return oData;
	}

	public static byte[] s2b(String str)
	{
		byte[] buf = null;
		try
		{
			buf = str.getBytes("ksc5601");
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return buf;
	}

	public static String b2s(byte[] buf)
	{
		return (null == buf) ? null : b2s(buf, 0, buf.length);
	}

	public static String b2s(byte[] buf, int bidx, int blen)
	{
		String str = null;
		try
		{
			str = new String(buf, bidx, blen, "ksc5601");
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return str;
	}

	public static String fmt(String str, int len, char ctype)
	{
		return format(str, len, ctype);
	}

	public static String fmt(int no, int len, char ctype)
	{
		return format(String.valueOf(no), len, ctype);
	}

	public static String format(String str, int len, char ctype)
	{
		byte[] buff;
		int filllen = 0;

		String trim_str = null;
		StringBuffer sb = new StringBuffer();

		buff = (str == null) ? new byte[0] : s2b(str);

		filllen = len - buff.length;
		if (filllen < 0)
		{
			for (int i = 0, j = 0; j < len - 4; i++)// 적당히 여유를 두고 잘라버리자
			{
				j += (str.charAt(i) > 127) ? 2 : 1;
				sb.append(str.charAt(i));
			}

			trim_str = sb.toString();
			buff = s2b(trim_str);
			filllen = len - buff.length;

			if (filllen <= 0)
				return new String(buff, 0, len);// 여기는 절대로 안타겠지...
			sb.setLength(0);
		} else
		{
			trim_str = str;
		}

		if (ctype == '9') // 숫자열인 경우
		{
			for (int i = 0; i < filllen; i++)
				sb.append('0');
			sb.append(trim_str);
		} else
		// 문자열인 경우
		{
			for (int i = 0; i < filllen; i++)
				sb.append(' ');
			sb.insert(0, trim_str);
		}
		return sb.toString();
	}
}
