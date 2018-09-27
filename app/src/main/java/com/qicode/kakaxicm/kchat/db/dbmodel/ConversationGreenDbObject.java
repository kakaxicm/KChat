package com.qicode.kakaxicm.kchat.db.dbmodel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by chenming on 2018/9/22
 */
@Entity
public class ConversationGreenDbObject {
//    自定义主键一定要设置主键名称为_id，和sqlite的默认主键保持一致
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long pid;
    @NotNull()
    private String id;//会话目标
    private String ext = "";
    private int unreadcount;
    private String latestmsgtext;
    private long latestmsgtimestamp;
    @Generated(hash = 1821482162)
    public ConversationGreenDbObject(Long pid, @NotNull String id, String ext,
            int unreadcount, String latestmsgtext, long latestmsgtimestamp) {
        this.pid = pid;
        this.id = id;
        this.ext = ext;
        this.unreadcount = unreadcount;
        this.latestmsgtext = latestmsgtext;
        this.latestmsgtimestamp = latestmsgtimestamp;
    }
    @Generated(hash = 1354172289)
    public ConversationGreenDbObject() {
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
    public String getExt() {
        return this.ext;
    }
    public void setExt(String ext) {
        this.ext = ext;
    }
    public int getUnreadcount() {
        return this.unreadcount;
    }
    public void setUnreadcount(int unreadcount) {
        this.unreadcount = unreadcount;
    }
    public String getLatestmsgtext() {
        return this.latestmsgtext;
    }
    public void setLatestmsgtext(String latestmsgtext) {
        this.latestmsgtext = latestmsgtext;
    }
    public long getLatestmsgtimestamp() {
        return this.latestmsgtimestamp;
    }
    public void setLatestmsgtimestamp(long latestmsgtimestamp) {
        this.latestmsgtimestamp = latestmsgtimestamp;
    }
}
