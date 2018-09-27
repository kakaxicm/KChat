package com.qicode.kakaxicm.kchat.enums;

/**
 * Created by chenming on 2018/9/20
 * Socket连接状态
 */
public enum SocketConnectStatus {
    SocketConnected,        // 连接成功
    SocketConnecting,       // 连接中
    SocketConnectError,     // 连接失败
    SocketDisconnected      // 连接断开
}
