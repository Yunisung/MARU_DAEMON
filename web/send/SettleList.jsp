<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt"%>
<div>
<c:forEach var="DATAMAP" items="${DATAMAP}" varStatus="status">
	<table style="margin: 0 auto; padding: 0px; width: 780px;" cellspacing="0" cellpadding="0">
		<tbody>
			<tr>
				<td><img style="border: 0px currentColor; border-image: none; width: 780px; vertical-align: top;"
				src="/assets/global/img/email-top.jpg"></td>
			</tr>
			<tr>
				<td>
					<table cellpadding="0" cellspacing="0" border="0" style="width: 780px; border-left: 1px #c4c4c4 solid; border-right: 1px #c4c4c4 solid; box-sizing: border-box; padding-left: 45px;">
						<tbody>
							<tr>
								<td style="width: 690px;">
									<table style="width: 690px;">
										<tbody>
											<tr>
												<td height="20">&nbsp;</td>
											</tr>
											<tr>
												<td style="font-family: 맑은고딕, Malgun Gothic; font-size: 35px; color: #333333; letter-spacing: -0.1em; line-height: 42px;">
												<span style="color: #005874;"> 월 정산내역을 안내</span>드립니다.</td>
											</tr>
											<tr>
												<td height="20">&nbsp;</td>
											</tr>
											<tr>
												<td style="font-family: 맑은고딕, Malgun Gothic; font-size: 14px; color: #666666; letter-spacing: -0.07em; line-height: 20px;">
													항상 (주)케이원 피에스 서비스를 이용해 주셔서 감사드립니다.</td>
											</tr>
											<tr>
												<td style="height: 20px; border-bottom: 1px #aaaaaa dotted;">&nbsp;</td>
											</tr>
											<tr>
												<td style="height: 20px;">&nbsp;</td>
											</tr>
										</tbody>
									</table>

									<table style="margin: 0px; width: 690px; border-collapse: collapse;">
										<tbody>
											<tr>
												<td style="font-family: 맑은고딕, Malgun Gothic; font-size: 18px; color: #333333; letter-spacing: -0.1em; margin: 0; padding-bottom: 10px;">
													상점정보</td>
												<td></td>
												<td></td>
											</tr>
											<tr style="height: 2px; background: #005874;">
												<td colspan="3"></td>
											</tr>
											<tr>
												<td style="background: #E8F6FA; padding: 10px; width: 30%; text-align: left; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid;">
													상점ID
												</td>
												<td style="width: 70%; text-align: left; color: #000000; padding-left: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid;">
													${DATAMAP.mchtId}</td>
												<td></td>
											</tr>
											<tr>
												<td style="background: #E8F6FA; padding: 10px; text-align: left; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid;">
													상호</td>
												<td style="text-align: left; color: #000000; padding-left: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid; -ms-word-break: break-all;">
													${DATAMAP.nick}</td>
												<td></td>
											</tr>
											<tr>
												<td style="background: #E8F6FA; padding: 10px; text-align: left; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid;">
													${DATAMAP.idType}</td>
												<td style="text-align: left; color: #000000; padding-left: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid; -ms-word-break: break-all;">
													${DATAMAP.identity}</td>
												<td></td>
											</tr>
											<tr>
												<td style="background: #E8F6FA; padding: 10px; text-align: left; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid;">
													계좌정보
												</td>
												<td style="text-align: left; color: #000000; padding-left: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom-color: #dbdbdb; border-bottom-width: 1px; border-bottom-style: solid; -ms-word-break: break-all;">
													${DATAMAP.bankName} ${DATAMAP.account} (예금주:${DATAMAP.accntHolder})
													</td>
												<td></td>
											</tr>
										</tbody>
									</table>
								</td>
							</tr>
							<tr>
								<td style="height: 10px;">&nbsp;</td>
							</tr>
							<tr>
								<td>
									<table style="margin: 0px; width: 690px; border-collapse: collapse;">
										<tbody>
											<tr>
												<td style="font-family: 맑은고딕, Malgun Gothic; font-size: 18px; color: #333333; letter-spacing: -0.1em; margin: 0; padding-bottom: 6px;">
													정산내역</td>
												<td></td>
												<td></td>
											</tr>
											<tr>
												<td colspan="2" style="padding-bottom: 6px; text-align: left; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px;">
													* 기간 : <span style="font-weight: bold;">
													${DATAMAP.startDay } ~ ${DATAMAP.endDay }
													</span>
												</td>
												<td style="padding-bottom: 6px; text-align: right; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px;">
													(단위 : 원)
												</td>
											</tr>
											<tr style="height: 2px; background: #005874;">
												<td colspan="3" style="border-collapse: collapse;"></td>
											</tr>
											<tr>
												<td
													style="background: #E8F6FA; padding: 10px; width: 33%; text-align: center; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													지급일자</td>
												<td
													style="background: #E8F6FA; width: 33%; text-align: center; color: #000000; padding-left: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													정산 대상 거래금액</td>
												<td
													style="background: #E8F6FA; padding: 10px; width: 33%; text-align: center; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb;">
													실제 지급금액</td>
											</tr>
											<tr>
												<td style="padding: 10px; width: 20%; text-align: center; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													${DATAMAP.startDay}
												</td>
												<td style="width: 30%; text-align: right; color: #000000; padding-right: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													${DATAMAP.payAmt}
												</td>
												<td style="padding: 10px; width: 20%; text-align: right; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb;">
													${DATAMAP.stlAmount}
												</td>
											</tr>
											<tr>
												<td style="padding: 10px; text-align: center; color: #000000; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													합계</td>
												<c:set var="sumAll" value="${sumAll + DATAMAP.payAmt + DATAMAP.rfdAmt}"/>	
												<td style="text-align: right; color: #000; padding-right: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; font-weight: bold; border-bottom: 1px solid #dbdbdb; border-right: 1px solid #dbdbdb;">
													<c:out value="${sumAll}"/></td>
												<c:set var="sumStl" value="${sumStl + DATAMAP.stlAmount}"/>
												<td style="text-align: right; color: #000; padding-right: 10px; font-family: 맑은고딕, Malgun Gothic; font-size: 12px; font-weight: bold; border-bottom: 1px solid #dbdbdb;">
													<c:out value="${sumStl}"/><td>
											</tr>
										</tbody>
									</table>

								</td>
							</tr>
							<tr>
								<td style="height: 10px;">&nbsp;</td>
							</tr>
							<tr>
								<td>
									<table style="font-family: 맑은고딕, Malgun Gothic; font-size: 12px; color: #666666; line-height: 17px;">
										<tbody>
											<tr>
												<td colspan="2" style="font-family: 맑은고딕, Malgun Gothic; font-size: 18px; color: #333333; letter-spacing: -0.1em; margin: 0; padding-bottom: 10px;">
													정산내역 안내사항</td>
											</tr>
											<tr>
												<td style="vertical-align: top;">*</td>
												<td>해당 일자별 상세내역은 <a target="_blank"
													href="http://onliner.allat.co.kr/servlet/MClick?SNO=271050927&amp;TID=10178&amp;APP=20210602094050&amp;JNO=0&amp;CNO=1&amp;ARG="
													style="color: red; text-decoration: underline;">정산내역 조회</a>를
													이용해 주시면 빠른 확인 가능합니다.
												</td>
											</tr>
											<tr>
												<td style="vertical-align: top;">*</td>
												<td>정산 대상 거래금액 : 해당 일자에 해당 상점ID로 정산된 총 거래 금액<br>
												<span style="padding-left: 115px;">= 해당 상점ID에 등록된 모든
														결제수단의 승인금액 - 취소금액</span></td>
											</tr>
											<tr>
												<td style="vertical-align: top;">*</td>
												<td>지급금액 : 해당 일자에 실제로 상점에 지급된 금액 <br>
												<span style="padding-left: 60px;">= 정산 대상 거래금액 –
														(수수료+부가세+미납금+지급보류 등)</span></td>
											</tr>
											<tr>
												<td style="vertical-align: top;">*</td>
												<td>지급 일자에 “정산 대상 거래금액＂ 또는 “실제 지급금액＂이 0원일 경우 표기되지 않습니다.</td>
											</tr>
										</tbody>
									</table>
								</td>
							</tr>
							<tr>
								<td style="height: 10px;">&nbsp;</td>
							</tr>
							<tr>
								<td>
									<table style="font-family: 맑은고딕, Malgun Gothic; font-size: 12px; line-height: 22px;"
										border="0" margin:="" 0px;="" width:="" 690px;="" border-collapse:="" collapse;"="">
										<tbody>
											<tr>
												<td style="vertical-align: top;"><img
													src="http://image.allat.co.kr/image/bizpay/pgmail2019/dot_bullet.gif"></td>
												<td>만약 메일 수신을 더 이상 원하지 않으시면, <a target="_blank"
													href="https://svc.mtouch.com/emailStatus?mchtId=${DATAMAP.mchtId}"
													style="color: #555; text-decoration: underline; font-weight: bold;">이메일
														수신거부</a>를 클릭해 주시기 바랍니다.
												</td>
											</tr>
											<tr>
												<td style="vertical-align: top;"><img
													src="http://image.allat.co.kr/image/bizpay/pgmail2019/dot_bullet.gif"></td>
												<td>If you don’t want this type of information or
													e-mail, <a target="_blank"
													href="http://onliner.allat.co.kr/servlet/MClick?SNO=271050927&amp;TID=10178&amp;APP=20210602094050&amp;JNO=0&amp;CNO=3&amp;ARG=HASH%3Dfda4f9f5a58f2781cecba05ba61780d1112591%26SYSDATE%3D20210602094050%26EMAIL%3Dmtouch@mtouch.com"
													style="color: #555; text-decoration: underline; font-weight: bold;">please
														click the refuse here</a>
												</td>
											</tr>
											<tr>
												<td style="vertical-align: top;"><img
													src="http://image.allat.co.kr/image/bizpay/pgmail2019/dot_bullet.gif"></td>
												<td>회원이 아니신 경우나 메일 수신거부 회원이신데도 메일을 받으셨다면, ☎ 1800-0678로
													문의주시기 바랍니다.</td>
											</tr>
											<tr>
												<td style="vertical-align: top;"><img
													src="http://image.allat.co.kr/image/bizpay/pgmail2019/dot_bullet.gif"></td>
												<td>상세 문의는 당사 홈페이지의 <a target="_blank"
													href="http://onliner.allat.co.kr/servlet/MClick?SNO=271050927&amp;TID=10178&amp;APP=20210602094050&amp;JNO=0&amp;CNO=4&amp;ARG=menu_id%3Dm040204"
													rel="noreferrer noopener"
													style="color: #555555; font-weight: bold;""="">1:1 상담</a>을
													이용해 주시면 빠른 답변을 받으실 수 있습니다.
												</td>
											</tr>
											<tr>
												<td style="vertical-align: top;"><img
													src="http://image.allat.co.kr/image/bizpay/pgmail2019/dot_bullet.gif"></td>
												<td>(주)케이원 피에스는 앞으로도 더 나은 서비스 제공을 위해 최선을 다하겠습니다.</td>
											</tr>
										</tbody>
									</table>
								</td>
							</tr>
							<tr>
								<td style="height: 10px;">&nbsp;</td>
							</tr>
						</tbody>
					</table>
				</td>
			</tr>
			<tr>
				<td><img style="border: 0px currentColor; border-image: none; width: 780px; vertical-align: top;"
				src="/assets/global/img/email-bottom.jpg"></td>
			</tr>
		</tbody>
	</table>
</c:forEach>
</div>