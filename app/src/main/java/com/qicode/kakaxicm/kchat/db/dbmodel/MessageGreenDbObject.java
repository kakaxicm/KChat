package com.qicode.kakaxicm.kchat.db.dbmodel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by chenming on 2018/9/22
 */
@Entity
public class MessageGreenDbObject {
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long pid;
    private String id;
    private long localtime;
    private long timestamp;
    private String conversation;
    private boolean receiver;
    private String chatType;
    private String bodies;
    private int sendStatus;
    @Generated(hash = 762029284)
    public MessageGreenDbObject(Long pid, String id, long localtime, long timestamp,
            String conversation, boolean receiver, String chatType, String bodies,
            int sendStatus) {
        this.pid = pid;
        this.id = id;
        this.localtime = localtime;
        this.timestamp = timestamp;
        this.conversation = conversation;
        this.receiver = receiver;
        this.chatType = chatType;
        this.bodies = bodies;
        this.sendStatus = sendStatus;
    }
    @Generated(hash = 867191895)
    public MessageGreenDbObject() {
    }
    public Long getPid() {
        return this.pid;
    }
    public void setPid(Long pid) {
        this.pid = pid;
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public long getLocaltime() {
        return this.localtime;
    }
    public void setLocaltime(long localtime) {
        this.localtime = localtime;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getConversation() {
        return this.conversation;
    }
    public void setConversation(String conversation) {
        this.conversation = conversation;
    }
    public boolean getReceiver() {
        return this.receiver;
    }
    public void setReceiver(boolean receiver) {
        this.receiver = receiver;
    }
    public String getChatType() {
        return this.chatType;
    }
    public void setChatType(String chatType) {
        this.chatType = chatType;
    }
    public String getBodies() {
        return this.bodies;
    }
    public void setBodies(String bodies) {
        this.bodies = bodies;
    }
    public int getSendStatus() {
        return this.sendStatus;
    }
    public void setSendStatus(int sendStatus) {
        this.sendStatus = sendStatus;
    }
}
