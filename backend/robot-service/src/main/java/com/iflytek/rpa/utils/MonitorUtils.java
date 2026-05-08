package com.iflytek.rpa.utils;

import com.iflytek.rpa.triggerTask.entity.dto.MailInfo;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.util.MailConnectException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

/**
 * Created by ieayoio on 16-9-19.
 */
@Slf4j
public class MonitorUtils {

    public static final String RECEIVED_HEADER_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String RECEIVED_HEADER_DATE_FORMAT1 = "E, dd MMM yyyy HH:mm:ss z";
    public static final String RECEIVED_HEADER_REGEXP = "^[^;]+;(.+)$";

    public static Date resolveReceivedDate(MimeMessage message) throws MessagingException {
        if (message.getReceivedDate() != null) {
            return message.getReceivedDate();
        }
        String[] receivedHeaders = message.getHeader("Received");
        if (receivedHeaders == null) {
            return message.getSentDate();
            //            return (Calendar.getInstance().getTime());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(RECEIVED_HEADER_DATE_FORMAT1, Locale.ENGLISH);
        Date finalDate = Calendar.getInstance().getTime();
        finalDate.setTime(0l);
        boolean found = false;
        for (String receivedHeader : receivedHeaders) {
            Pattern pattern = Pattern.compile(RECEIVED_HEADER_REGEXP);
            Matcher matcher = pattern.matcher(receivedHeader);
            if (matcher.matches()) {
                String regexpMatch = matcher.group(1);
                if (regexpMatch != null) {
                    regexpMatch = regexpMatch.trim();
                    try {
                        Date parsedDate = sdf.parse(regexpMatch);
                        if (parsedDate.after(finalDate)) {
                            // finding the first date mentioned in received header
                            finalDate = parsedDate;
                            found = true;
                        }
                    } catch (ParseException e) {
                        log.debug("메일 Received 헤더 날짜 파싱 실패: {}", regexpMatch, e);
                    }
                } else {
                }
            }
        }

        //        return found ? finalDate : Calendar.getInstance().getTime();
        return found ? finalDate : message.getSentDate();
    }

    public static SearchTerm getSearch() {
        // 에서현재날짜열기 조회까지초
        Date today = new Date();
        Date end = DateUtils.getCalMinute(today, 1440);
        // 조회까지현재시간
        Date start = DateUtils.getCalMinute(today, 0);
        return new AndTerm(
                new ReceivedDateTerm(ComparisonTerm.GE, start), new ReceivedDateTerm(ComparisonTerm.LE, end));
    }

    public static SearchTerm getSearch1() {
        // 에서현재날짜열기 조회까지초
        Date today = new Date();
        Date end = DateUtils.getCalMinute(today, 100);
        // 조회까지현재시간
        Date start = DateUtils.getCalMinute(today, -100);
        return new AndTerm(new SentDateTerm(ComparisonTerm.GE, start), new SentDateTerm(ComparisonTerm.LE, end));
    }

    public static List<MailInfo> getMailInfos(
            String type, Integer port, String host, boolean sslEnable, String mail, String password) {
        log.info("메일 모니터 연결 테스트 시작: {}", mail);
        try {

            Properties props = System.getProperties();
            //            props.put("mail.pop3.ssl.enable", true);
            //            props.put("mail.pop3.host","pop.qq.com");
            //            props.put("mail.pop3.port",995);
            props.put("mail." + type + ".ssl.enable", sslEnable);
            if (StringUtils.isNotEmpty(host)) {
                props.put("mail." + type + ".host", host);
            }
            if (port != null) {
                props.put("mail." + type + ".port", port);
            }

            //            Session session1 = Session.getInstance(System.getProperties(),null);
            Session session1 = Session.getInstance(props, null);
            //            Store store1 = session1.getStore("pop3");
            Store store1 = session1.getStore(type);
            IMAPStore store = null;
            Folder inbox = null;
            try {
                store1.connect(mail, password);
                if ((mail.contains("163.com") || mail.contains("126.com")) && "imap".equals(type)) {
                    Map<String, String> IAM = new HashMap<>();
                    // 위IMAP ID정보, key및value그룹성공, 예name, version, vendor, support-email대기.
                    // 개value의값행
                    IAM.put("name", "myname");
                    IAM.put("version", "1.0.0");
                    IAM.put("vendor", "myclient");
                    IAM.put("support-email", "testmail@test.com");
                    store = (IMAPStore) store1;
                    store.id(IAM);
                    inbox = store.getFolder("INBOX");
                } else {
                    inbox = store1.getFolder("INBOX");
                }

                inbox.open(Folder.READ_ONLY);
                FetchProfile profile = new FetchProfile();
                profile.add(UIDFolder.FetchProfileItem.UID);
                profile.add(FetchProfile.Item.ENVELOPE);
                Message[] messages = "pop3".equals(type) ? inbox.search(getSearch1()) : inbox.search(getSearch());
                List<MailInfo> mailInfos = new ArrayList<>();
                for (int i = 0; i < messages.length; i++) {
                    MailInfo mailInfo = new MailInfo();
                    if (messages[i].isMimeType("multipart/*")) {
                        Multipart mp = (Multipart) messages[i].getContent();
                        int bodyNum = mp.getCount();
                        StringBuilder contentStr = new StringBuilder();
                        for (int k = 0; k < bodyNum; k++) {
                            if (mp.getBodyPart(k).isMimeType("text/*")) {
                                String content = (String) mp.getBodyPart(k).getContent();
                                contentStr.append(Jsoup.parse(content).text());
                            } else if (mp.getBodyPart(k).isMimeType("multipart/*")) {
                                Multipart mp1 = (Multipart) mp.getBodyPart(k).getContent();
                                int bodyNum1 = mp1.getCount();
                                for (int q = 0; q < bodyNum1; q++) {
                                    if (mp1.getBodyPart(q).isMimeType("text/*")) {
                                        String content =
                                                (String) mp1.getBodyPart(q).getContent();
                                        contentStr.append(Jsoup.parse(content).text());
                                    }
                                }
                            }
                        }
                        mailInfo.setContent(contentStr.toString());
                    }
                    List<String> fromAddresses = new ArrayList<>();
                    List<String> toAddresses = new ArrayList<>();
                    for (Address addresses : messages[i].getFrom()) {
                        InternetAddress address = (InternetAddress) addresses;
                        fromAddresses.add(address.getAddress());
                    }
                    for (Address addresses : messages[i].getAllRecipients()) {
                        InternetAddress address = (InternetAddress) addresses;
                        toAddresses.add(address.getAddress());
                    }
                    mailInfo.setFromAddresses(fromAddresses);
                    mailInfo.setToAddresses(toAddresses);
                    //
                    // mailInfo.setIsAttachment(messages[i].getContentType().contains("multipart/MIXED;"));
                    try {
                        mailInfo.setIsAttachment(handleMultipart(messages[i]));
                    } catch (Exception e) {
                        mailInfo.setIsAttachment(false);
                    }
                    mailInfo.setTitle(messages[i].getSubject());
                    //                    mailInfo.setContentType(messages[i].getContentType());
                    if ("pop3".equals(type)) {
                        mailInfo.setTime(resolveReceivedDate((MimeMessage) messages[i]));
                    } else {
                        mailInfo.setTime(messages[i].getReceivedDate());
                    }

                    mailInfos.add(mailInfo);

                }
                log.info("메일 모니터 조회 결과: {}건", mailInfos.size());
                return mailInfos;
            } catch (Exception e) {
                log.warn("메일 목록 조회 실패: {}", e.getMessage(), e);
            } finally {
                try {
                    inbox.close(false);
                } catch (Exception e) {
                }
                try {
                    store1.close();
                    if (store != null) {
                        store.close();
                    }
                } catch (Exception e) {
                }
            }

        } catch (Exception ex) {
            log.warn("메일 모니터 초기화 실패: {}", ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }

    public static String connectMail(
            String type, Integer port, String host, boolean sslEnable, String mail, String password) {
        log.info("connect mail");
        log.info("mail----" + mail);
        try {
            Properties props = System.getProperties();
            //            props.put("mail.pop3.ssl.enable", true);
            //            props.put("mail.pop3.host","pop.qq.com");
            //            props.put("mail.pop3.port",995);
            props.put("mail." + type + ".ssl.enable", sslEnable);
            props.put("mail." + type + ".ssl.trust", "*");
            if (StringUtils.isNotEmpty(host)) {
                props.put("mail." + type + ".host", host);
            }
            if (port != null) {
                props.put("mail." + type + ".port", port);
            }

            //            Session session1 = Session.getInstance(System.getProperties(),null);
            Session session1 = Session.getInstance(props, null);
            //            Store store1 = session1.getStore("pop3");
            Store store1 = session1.getStore(type);
            try {
                store1.connect(mail, password);
                return "";
            } catch (MailConnectException e) {
                log.warn("메일함 서비스 연결 실패: {}", e.getMessage(), e);
                return "메일함서비스주소및단말있음제목";
            } catch (AuthenticationFailedException e1) {
                log.warn("메일 인증 실패: {}", e1.getMessage(), e1);
                return "인증실패";
            } catch (Exception e2) {
                log.warn("메일 연결 예외: {}", e2.getMessage(), e2);
                return "예외";
            } finally {
                try {
                    store1.close();
                } catch (Exception e) {
                }
            }

        } catch (Exception ex) {
            log.warn("메일 연결 초기화 예외: {}", ex.getMessage(), ex);
            return "예외";
        }
    }

    private static boolean handleMultipart(Message msg) throws Exception {
        String disposition;
        BodyPart part;
        Multipart mp = (Multipart) msg.getContent();
        // Miltipart의수,사용제거완료다중개part,예다중개파일
        int mpCount = mp.getCount();
        for (int m = 0; m < mpCount; m++) {
            //            this.handle(msg);
            part = mp.getBodyPart(m);
            disposition = part.getDisposition();
            // 여부있음파일
            if (disposition != null && disposition.equals(Part.ATTACHMENT)) {
                // 에서파일의저장
                return true;
            }
        }
        return false;
    }

    public static boolean hasAttachment(Message message) throws MessagingException {
        MimeMessage mimeMessage = (MimeMessage) message;
        return true;
        //        boolean hasAttachments = false;
        //
        //        for (int i = 0; i < mimeMessage.getDataHandler(); i++) {
        //
        //            if (message.getDataHandler(i).isDataSource()) {
        //
        //                hasAttachments = true;
        //
        //                break;
        //
        //            }
        //
        //        }
        //        return hasAttachments;
    }
}
