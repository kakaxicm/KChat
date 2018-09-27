package com.qicode.kakaxicm.kchat.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.qicode.kakaxicm.kchat.db.dbmodel.ConversationGreenDbObject;
import com.qicode.kakaxicm.kchat.db.dbmodel.ConversationGreenDbObjectDao;
import com.qicode.kakaxicm.kchat.db.dbmodel.DaoMaster;
import com.qicode.kakaxicm.kchat.db.dbmodel.DaoSession;
import com.qicode.kakaxicm.kchat.db.dbmodel.MessageGreenDbObject;
import com.qicode.kakaxicm.kchat.db.dbmodel.MessageGreenDbObjectDao;
import com.qicode.kakaxicm.kchat.enums.ChatType;
import com.qicode.kakaxicm.kchat.enums.MessageSendStatus;
import com.qicode.kakaxicm.kchat.manager.ClientManager;
import com.qicode.kakaxicm.kchat.model.MessageBody;
import com.qicode.kakaxicm.kchat.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

import static com.qicode.kakaxicm.kchat.enums.MessageSendStatus.MessageSendFail;
import static com.qicode.kakaxicm.kchat.enums.MessageSendStatus.MessageSending;

/**
 * Created by chenming on 2018/9/22
 */
public class DbManager {
    private static DbManager sInstance = new DbManager();

    public static DbManager getsInstance() {
        return sInstance;
    }

    private DbManager() {

    }

    private String mDbName = null;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private DaoMaster.DevOpenHelper mDevOpenHelper;
    private DaoSession mDaoSession;
    //消息和会话dao
    private static MessageGreenDbObjectDao mMessageDao;
    private static ConversationGreenDbObjectDao mConversationDao;

    public void createDb(Context context, String userName) {
        mContext = context;
        if (mDbName != null && mDbName.equals(userName)) {
            return;
        }
        mDbName = userName;

        setUpSession();
    }

    //初始化DAO
    private void setUpSession() {
        if (!TextUtils.isEmpty(ClientManager.currentUserId)) {
            mDevOpenHelper =
                    new DaoMaster.DevOpenHelper(mContext, mDbName + ".db", null);
            SQLiteDatabase db = mDevOpenHelper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            mDaoSession = daoMaster.newSession();
            mMessageDao = mDaoSession.getMessageGreenDbObjectDao();
            mConversationDao = mDaoSession.getConversationGreenDbObjectDao();
        }
    }

    /*
     *
     * 消息数据转数据库
     * */
    private MessageGreenDbObject messageToDbObject(MessageModel messageModel) {
        //GreenDaoModel
        MessageGreenDbObject dbObject = new MessageGreenDbObject();
        dbObject.setId(messageModel.getMsg_id());
        dbObject.setLocaltime(messageModel.getSendTime());
        dbObject.setTimestamp(messageModel.getTimestamp());
        //"我"是不是接收者
        boolean isReceiver = mDbName.equals(messageModel.getTo_user());
        //会话对象
        dbObject.setConversation(isReceiver ? messageModel.getFrom_user() : messageModel.getTo_user());
        dbObject.setReceiver(isReceiver);
        dbObject.setChatType("chat");
        String bodies = JSON.toJSONString(messageModel.getBodies());
        dbObject.setBodies(bodies);
        int statusId = 0;
        switch (messageModel.getSendStatus()) {
            case MessageSending:
                statusId = 0;
                break;
            case MessageSendFail:
                statusId = 1;
                break;
            case MessageSendSuccess:
                statusId = 2;
                break;
        }
        dbObject.setSendStatus(statusId);

        return dbObject;
    }


    /*
     *
     * 插入消息数据
     * */
    public void insertMessage(final MessageModel messageModel) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MessageGreenDbObject dbMessageObj = messageToDbObject(messageModel);
                mMessageDao.insertInTx(dbMessageObj);
            }
        });

    }

    /*
     * 更新消息数据
     * */
    public void updateMessage(final MessageModel messageModel) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MessageGreenDbObject messageGreenDbObject = mMessageDao
                        .queryBuilder()
                        .where(MessageGreenDbObjectDao.Properties.Localtime.eq(messageModel.getSendTime()))
                        .build()
                        .unique();
                if (messageGreenDbObject != null) {
                    messageGreenDbObject.setId(messageModel.getMsg_id());
                    messageGreenDbObject.setTimestamp(messageModel.getTimestamp());
                    messageGreenDbObject.setBodies(JSON.toJSONString(messageModel.getBodies()));
                    int status = 0;
                    switch (messageModel.getSendStatus()) {
                        case MessageSending:
                            status = 0;
                            break;
                        case MessageSendFail:
                            status = 1;
                            break;

                        case MessageSendSuccess:
                            status = 2;
                            break;
                    }
                    messageGreenDbObject.setSendStatus(status);
                    mMessageDao.update(messageGreenDbObject);
                }
            }
        });
    }

    /*
     * 插入或者更新会话消息
     * */
    public void insertOrUpdateConversation(final MessageModel messageModel) {

        String conversationName = mDbName.endsWith(messageModel.getFrom_user()) ? messageModel.getTo_user() : messageModel.getFrom_user();

        conversationIsExist(conversationName, new SearchExistCallBack() {
            @Override
            public void findResult(boolean isExist) {

                if (isExist) { // 会话存在， 更新最新消息及时间

                    updateConversation(messageModel);
                } else { // 会话不存在，新增会话

                    insertConversation(messageModel);
                }
            }
        });
    }

    /*
     *
     * 判断会话是否存在
     * @param
     * */
    private void conversationIsExist(final String name,
                                     final SearchExistCallBack callBack) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                //根据会话目标查询会话
                ConversationGreenDbObject conversationGreenDbObject = mConversationDao
                        .queryBuilder()
                        .where(ConversationGreenDbObjectDao.Properties.Id.eq(name))
                        .build()
                        .unique();
                callBack.findResult(conversationGreenDbObject != null);
            }
        });

    }


    /*
     * 消息模型转会话数据库对象
     * */
    public ConversationGreenDbObject messageToConversationDb(MessageModel messageModel) {

        ConversationGreenDbObject dbObject = new ConversationGreenDbObject();

        String conversationName = messageModel.getFrom_user().endsWith(mDbName) ? messageModel.getTo_user() : messageModel.getFrom_user();
        dbObject.setId(conversationName);
        boolean isChatting = ClientManager.isChattingWithUser(conversationName);
        int unreadCount = isChatting ? 0 : 1;
        dbObject.setUnreadcount(unreadCount);
        dbObject.setLatestmsgtext(messageModel.getMessageTip());
        dbObject.setLatestmsgtimestamp(messageModel.getTimestamp());
        return dbObject;
    }


    /*
     * 新增会话
     * */
    private void insertConversation(final MessageModel messageModel) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ConversationGreenDbObject dbConversationObj = messageToConversationDb(messageModel);
                mConversationDao.insertInTx(dbConversationObj);


            }
        });
    }

    /*
     * 更新会话
     * */
    public void updateConversation(final MessageModel messageModel) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                String conversationName = messageModel.getFrom_user().endsWith(mDbName) ? messageModel.getTo_user() : messageModel.getFrom_user();
                ConversationGreenDbObject conversationGreenDbObject = mConversationDao
                        .queryBuilder()
                        .where(ConversationGreenDbObjectDao.Properties.Id.eq(conversationName))
                        .build()
                        .unique();
                if (conversationGreenDbObject != null) {
                    conversationGreenDbObject.setLatestmsgtext(messageModel.getMessageTip());
                    conversationGreenDbObject.setLatestmsgtimestamp(messageModel.getTimestamp());
                    boolean isChatting = ClientManager.isChattingWithUser(conversationName);
                    conversationGreenDbObject.setUnreadcount(isChatting ? 0 : conversationGreenDbObject.getUnreadcount() + 1);
                    //更新会话
                    mConversationDao.updateInTx(conversationGreenDbObject);
                }
            }
        });
    }

    /*
     * 查询所有会话
     * */
    public void queryAllConversation(
            final QueryDbCallBack<ConversationGreenDbObject> callBack) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                //按时间戳降序排列
                if(mConversationDao == null){
                    setUpSession();
                }
                List<ConversationGreenDbObject> results = mConversationDao
                        .queryBuilder()
                        .orderDesc(ConversationGreenDbObjectDao.Properties.Latestmsgtimestamp)
                        .list();
                if(results != null){
                    callBack.querySuccess(results, false);
                }
            }
        });
    }

    // 分页查询与某个用户的聊天信息
    public void queryMessages(final String username, final int page, final int limit,
                              final QueryDbCallBack<MessageModel> callBack) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                //按时间戳降序排列
                List<MessageGreenDbObject> results = mMessageDao
                        .queryBuilder()
                        .where(MessageGreenDbObjectDao.Properties.Conversation.eq(username))
                        .orderDesc(MessageGreenDbObjectDao.Properties.Timestamp)
                        .offset(page * limit)
                        .limit(limit)
                        .list();
                long totalCount = mMessageDao.queryBuilder().where(MessageGreenDbObjectDao.Properties.Conversation.eq(username)).count();
                int startIndex = page * limit;
                if(results != null){
                    if(totalCount < startIndex){// 没有分页数据了
                        callBack.querySuccess(null, false);
                    }else{
                        if (startIndex + limit < totalCount) { // 还有更多数据
                            //当前页面的数据转化成消息体
                            List<MessageModel> messages = new ArrayList<>();
                            for (int i = 0; i < limit; i++) {
                                MessageGreenDbObject dbObject = results.get(i);
                                messages.add(0, DBModelToMessageModel(dbObject));
                            }

                            callBack.querySuccess(messages, true);
                        }else{//这是最后一页了
                            List<MessageModel> messages = new ArrayList<>();
                            for(int i = 0; i < results.size(); i++){
                                MessageGreenDbObject dbObject = results.get(i);
                                messages.add(0, DBModelToMessageModel(dbObject));
                            }
                            callBack.querySuccess(messages, false);
                        }
                    }
                }else{
                    callBack.querySuccess(null, false);
                }
            }
        });
    }

    /*
     * 数据库消息转聊天消息模型
     * */
    private static MessageModel DBModelToMessageModel(MessageGreenDbObject dbObject) {

        MessageModel msg = new MessageModel();

        msg.setMsg_id(dbObject.getId());
        msg.setSendTime(dbObject.getLocaltime());
        msg.setTimestamp(dbObject.getTimestamp());

        boolean isReceiver = dbObject.getReceiver();
        String conversation = dbObject.getConversation();
        String currentUser = ClientManager.currentUserId;
        msg.setFrom_user(isReceiver ? conversation : currentUser);
        msg.setTo_user(isReceiver ? currentUser : conversation);
        msg.setChat_type(ChatType.chat);

        MessageSendStatus status = MessageSendStatus.MessageSendFail;
        switch (dbObject.getSendStatus()) {
            case 0:
                status = MessageSendStatus.MessageSending;
                break;
            case 1:
                status = MessageSendStatus.MessageSendFail;
                break;
            case 2:
                status = MessageSendStatus.MessageSendSuccess;
                break;
        }
        msg.setSendStatus(status);
        MessageBody body = JSON.parseObject(dbObject.getBodies(), MessageBody.class);
        msg.setBodies(body);

        return msg;
    }

    /*
     * 清空会话未读消息
     * */
    public void clearConversationUnreadCount(final String conversationId) {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ConversationGreenDbObject dbObject = mConversationDao.queryBuilder().where(ConversationGreenDbObjectDao.Properties.Id.eq(conversationId)).build().unique();
                if(dbObject != null){
                    dbObject.setUnreadcount(0);
                    mConversationDao.update(dbObject);
                }

            }
        });
    }

    // 查询的回调
    private interface SearchExistCallBack {

        void findResult(boolean isExist);
    }

    public interface QueryDbCallBack<T> {

        void querySuccess(List<T> items, boolean hasMore);
    }

}
