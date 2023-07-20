package com.pgmate.dm.util;

import com.jcraft.jsch.*;
import com.pgmate.dm.exception.DiffTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.util.Vector;

public class SFTPUtil {
    private static Logger logger = LoggerFactory.getLogger(SFTPUtil.class);
    private Session session = null;
    private Channel channel = null;
    private ChannelSftp channelSftp = null;

    /**
     * 서버 연결에 필요한 값들을 가져와 초기화
     *
     * @param host 서버 주소
     * @param userId 아이디
     * @param userPw 패스워드
     * @param port 포트번호
     */
    public void init(String host, String userId, String userPw, int port) throws DiffTransportException {
        JSch jsch = new JSch();

        logger.info("===== CONNECTING START =====");
        try {
            session = jsch.getSession(userId, host, port);
            session.setPassword(userPw);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            logger.info("CONNECTED TO ===> {}", host);
            channel = session.openChannel("sftp");
            channel.connect();
        } catch (JSchException e) {
            //logger.info("CONNECTED FAIL");
            logger.error("SFTPUTil CONNECTED FAIL ", e);
            throw new DiffTransportException(e);
        }

        channelSftp = (ChannelSftp) channel;
    }

    /**
     * 디렉토리( or 파일) 존재 여부
     *
     * @param path 디렉토리 (or 파일)
     * @return
     */
    public boolean exists(String path) throws DiffTransportException {
        Vector res = null;
        try {
            res = channelSftp.ls(path);
            logger.info("===== GALAXIA FILE EXIST TRUE =====");
        } catch (SftpException e) {
            if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                logger.error("GALAXIA FILE EXIST FALSE  ", e);
                throw new DiffTransportException(e);
            }
        }

        if(res != null && !res.isEmpty()){
            logger.info("===== GALAXIA FILE IS NOT NULL =====");
            return true;
        }

        logger.info("===== GALAXIA FILE IS NULL =====");
        return false;
    }

    /**
     * 파일 업로드
     *
     * @param dir 저장할 디렉토리
     * @param file 저장할 파일
     * @return 업로드 여부
     */
    public boolean upload(String dir, File file) throws DiffTransportException {
        logger.info("SFTP FILE UPLOAD PATH =====> {}", dir);
        boolean isUpload = false;
        SftpATTRS sftpATTRS;
        FileInputStream in = null;

        try {
//            String fileName = URLEncoder.encode(file.getName(), "EUC-KR");
            in = new FileInputStream(file);
            channelSftp.cd(dir);
            channelSftp.put(in, file.getName());
//            channelSftp.put(in, fileName);

            if(this.exists(dir + "/" + file.getName())) {
                isUpload = true;
            }
        } catch (Exception e) {
            logger.error("SFTP FILE UPLOAD ERROR", e);
            throw new DiffTransportException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                logger.error("SFTP FILE INPUTSTREAM CLOSE ERROR", e);
                throw new DiffTransportException(e);
            }
        }
        return isUpload;
    }

    public void download(String dir, String downloadFile, String path) throws DiffTransportException {
        logger.info("SFTP FILE DOWNLOAD PATH : {}", path);
        InputStream in = null;
        FileOutputStream out = null;

        try {
            channelSftp.cd(dir);
            in = channelSftp.get(downloadFile);
        } catch (SftpException e) {
            e.printStackTrace();
        }

        try {
            out = new FileOutputStream(new File(path));
            int i;

            while ((i = in.read()) != -1) {
                out.write(i);
            }
        } catch (IOException e) {
            logger.info("SFTP FILE DOWNLOAD FAIL", e);
            throw new DiffTransportException(e);
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                logger.info("SFTP FILE DOWNLOAD IN CLOSE FAIL", e);
                throw new DiffTransportException(e);
            }
        }
    }

    /**
     * 연결 종료
     */
    public void disconnection() {
        if(channelSftp != null) channelSftp.quit();
        if(session != null) session.disconnect();
    }
}
