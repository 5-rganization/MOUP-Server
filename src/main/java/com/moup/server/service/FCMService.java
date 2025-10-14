package com.moup.server.service;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FCMService {

    /**
     * 전체 공지 떄리기
     * 
     * @param token
     * @param title
     * @param body
     * @throws FirebaseMessagingException
     */
    public void sendNotification(String token, String title, String body) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                // .setImage("url-to-image") // 이미지 추가 가능
                .build();

        Message message = Message.builder()
                .setToken(token) // 특정 기기(클라이언트)의 토큰
                .setNotification(notification)
                // .putData("key", "value") // 데이터 페이로드 추가 가능
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        System.out.println("Successfully sent message: " + response);
    }
}
