package com.qicode.kakaxicm.kchat.manager;

/**
 * Created by chenming on 2018/9/20
 * 记录当前的用户状态
 */
public class ClientManager {
    //当前登录的客户
    public static String currentUserId = "";
    //当前正在聊天的对方id
    public static String chattingUserId = "";

    //是否正在与对方聊天
    public static boolean isChattingWithUser(String user) {

        return user.equals(ClientManager.chattingUserId);
    }
}
