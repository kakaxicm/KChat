package com.qicode.kakaxicm.kchat.model;

import com.qicode.kakaxicm.kchat.enums.MessageEventType;

/**
 * Created by chenming on 2018/9/20
 * EB总线上的消息,封装了服务器发来的各种消息
 */
public class MessageEvent {
    //消息类型
    private MessageEventType type;
    //消息体
    private Object msg;

    public MessageEvent(MessageEventType type, Object msg) {
        this.type = type;
        this.msg = msg;
    }

    public MessageEventType getType() {
        return type;
    }

    public Object getMsg() {
        return msg;
    }
}
