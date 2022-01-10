package com.pgmate.dm.main;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.SettleMchtDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class SettleSendEmail {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.SettleSendEmail.class);
	private SmsGw smsGw = null;
	private String msgBody = "";

	public static void main(String[] args) throws Exception {
		new SettleSendEmail();
	}

	public SettleSendEmail() throws Exception {
		logger.info("==================================================");
		logger.info("SettleSendEmail START");

		SettleSendList();

		logger.info("==================================================");
		logger.info("SettleSendEmail END");
	}

	// 월 정산내역을 가맹점 이메일로 전송. 매월 2일 오전 10시 전송한다.
	public void SettleSendList() {
		logger.info("==================================================");
		logger.info("SettleSendList START");
		
		SettleMchtDAO dao = new SettleMchtDAO();

		try {
			//지난 달 1일 ~ 말일
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String current = CommonUtil.getCurrentDate("yyyyMMdd");

			cal.set(Calendar.YEAR, Integer.parseInt(current.substring(0, 4)));
			cal.add(Calendar.MONTH, -1);
			cal.set(Calendar.DATE, 1);
			String startDay = sdf.format(cal.getTime());

			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMM");
			String getEndDay = sdf2.format(cal.getTime());
			String endDay = String.valueOf(CommonUtil.getLastDayOfMonth(getEndDay));

			//이전 전송 내역 삭제
			String getSendDay = current;
			//dao.deleteBefore(getSendDay);
			
			// 발송 대상 가맹점 LIST
			List<SharedMap<String, Object>> idList = dao.getSendMchtId(startDay, getEndDay+endDay);
			SharedMap<String, Object> map = new SharedMap<String, Object>();

			// 가맹점 LIST 만큼 정산 내역 데이터 mapping 및 이메일 발송
			if(idList.size() > 0) {
				logger.info("==================================================");
				logger.info("SettleSendList COUNT : {}", idList.size());
				
				for(SharedMap<String,Object> id : idList) {
					String recipient = id.getString("mchtId");	//전송 가맹점
					String address = dao.getEmail(recipient);	//이메일 주소
					String name = dao.getNick(recipient);		//가맹점 명
					logger.info("---SettleSend mchtId : [{}], address : [{}], sendDay : [{}]", recipient, address, getSendDay);
					
					//정산 내역 mapping
					String getData = mappingSettle(recipient, startDay, getEndDay+endDay);
					
					map.put("mchtId"			, recipient);
					map.put("name"				, name);
					map.put("email"				, address);
					map.put("startDay"			, startDay);
					map.put("endDay"			, getEndDay+endDay);
					
					//이메일 전송
					if(sendEmail(getData, name, address)) {
						dao.insertSettleList(map);
					} else {
						msgBody = "월 정산내역 이메일 전송 실패 : [" + recipient + "], [" + address + "], [" + startDay + " ~ " + getEndDay+endDay + "]";
						logger.info(msgBody);
					}
				}
			}
			logger.info("==================================================");
			logger.info("SettleSendList END");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			msgBody = "월 정산내역 이메일 전송 중 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
			logger.info(msgBody);
		}
	}

	// 정산 내역 mapping
	public String mappingSettle(String mchtId, String startDay, String endDay) {
		logger.info("==================================================");
		logger.info("mappingSettle START");

		String data = "";
		String path = "";
		
		try {
			logger.info("mappingSettle id : [{}], startDay : [{}], endDay : [{}]", mchtId, startDay, endDay);
			
			path = "http://pgwas1:10001/mcht/doc/settleSendEmail?mchtId=" + mchtId + "&startDay=" + startDay + "&endDay=" + endDay;
			logger.info("mappingSettle path : " + path);
			
			Document doc = Jsoup.connect(path).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
					.timeout(30000).get();
			data = doc.html();
			
			logger.info("mappingSettle END");
			logger.info("==================================================");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("정산내역 매핑 중 오류발생. 확인요망 [" + e.getMessage() + "]");
		}
		return data;
	}

	// 이메일 전송 연동 java-mail.jar
	public boolean sendEmail(String getData, String name, String address) throws AddressException, MessagingException {
		logger.info("==================================================");
		logger.info("sendEmail START");

		String host = "outbound.daouoffice.com";
		String port = "465";

		String fromId = "MARUps@mtouch.com";			// 발신자
		String fromPw = "mtouch1855!";					// 발신자 비밀번호
		String fromName = "[(주)케이원 피에스]";				// 발신자 정보
		String to = address;							// 수신자
		String subject = "[(주)케이원 피에스] (" + name + ") 월 정산내역 안내";	// 이메일 제목

		try {
			Properties props = System.getProperties();

			// SMTP 서버 정보 설정
			props.put("mail.transport.protocol", "smtp");
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.port", port);
			props.put("mail.smtp.auth", "true");

			props.put("mail.smtp.ssl.enable", "true");
			props.put("mail.smtp.ssl.trust", host);

			// 발신자 메일 서버 인증
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(fromId, fromPw);
				}
			};

			// 메일 세션 생성
			Session session = Session.getInstance(props, auth);
			session.setDebug(true);
			
			// 메일 송/수신 옵션 설정
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(fromId, fromName));						//발신자
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));	//수신자
			//msg.setRecipients(Message.RecipientType.TO, toAddr);					//다수의 수신자
			
			msg.setSubject(subject);
			msg.setContent(getData, "text/html; charset=UTF-8");

			Transport.send(msg);
			
			logger.info("==================================================");
			logger.info("sendEmail END");
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("월 정산내역 이메일 연동 중 오류발생. 확인요망 [" + e.getMessage() + "]");
			
			return false;
		}
		return true;
	}
}