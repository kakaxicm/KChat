package com.qicode.kakaxicm.kchat.manager;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.TimeUtils;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.enums.ChatType;
import com.qicode.kakaxicm.kchat.enums.MessageSendStatus;
import com.qicode.kakaxicm.kchat.enums.MessageType;
import com.qicode.kakaxicm.kchat.model.MessageBody;
import com.qicode.kakaxicm.kchat.model.MessageModel;
import com.qicode.kakaxicm.kchat.util.KUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import io.socket.client.Ack;

/**
 * Created by chenming on 2018/9/25
 */
public class ChatManager {
    /**
     * 发送文本消息
     * @param text
     * @param toUser
     * @param callBack
     * @return
     */
    public static MessageModel sendTextMsg(String text, String toUser, SendStatusCallBack callBack) {
        MessageBody body = new MessageBody();
        body.setType(MessageType.txt);
        body.setMsg(text);
        MessageModel messageModel = new MessageModel();
        messageModel.setBodies(body);

        return sendMsg(messageModel, toUser, callBack);

    }

    /**
     * 发送音频
     *
     * @param audioName 音频文件名
     * @param duration  时长
     * @param toUser    发送目标
     * @param callBack  发送回调
     * @return
     */
    public static MessageModel sendAudioMsg(String audioName, long duration, String toUser, SendStatusCallBack callBack) {

        MessageBody body = new MessageBody();
        body.setType(MessageType.audio);
        body.setFileName(audioName);
        body.setDuration(duration);
        MessageModel messageModel = new MessageModel();
        messageModel.setBodies(body);

        return sendMsg(messageModel, toUser, callBack);
    }

    /**
     * 发送图片
     * @param imagePath
     * @param imageName
     * @param size
     * @param toUser
     * @param callBack
     * @return
     */
    public static MessageModel sendImageMsg(String imagePath, String imageName, HashMap size, String toUser, SendStatusCallBack callBack) {

        MessageBody body = new MessageBody();
        body.setType(MessageType.img);

        boolean saveImage = imageName == null;
        imageName = imageName == null ? KUtil.createUUID() + ".jpg" : imageName;
        body.setFileName(imageName);
        body.setSize(size);
        body.setOriginImagePath(imagePath);
        MessageModel messageModel = new MessageModel();
        messageModel.setBodies(body);

        return sendMsg(messageModel, saveImage, toUser, callBack);
    }

    private static MessageModel sendMsg(final MessageModel messageModel, String toUser, final SendStatusCallBack callBack) {

        return sendMsg(messageModel, true, toUser, callBack);
    }

    public static MessageModel sendLocationMsg(double lat, double lon, String location, String detail, String toUser, SendStatusCallBack callBack) {

        MessageBody body = new MessageBody();
        body.setType(MessageType.loc);
        body.setLatitude(lat);
        body.setLongitude(lon);
        body.setLocationName(location);
        body.setDetailLocationName(detail);

        MessageModel messageModel = new MessageModel();
        messageModel.setBodies(body);

        return sendMsg(messageModel, toUser, callBack);
    }

    /**
     * 发送消息核心代码
     *
     * @param messageModel
     * @param saveImage
     * @param toUser
     * @param callBack
     * @return
     */
    private static MessageModel sendMsg(final MessageModel messageModel, boolean saveImage, String toUser, final SendStatusCallBack callBack) {
        //刚发送的消息为正在发送状态
        messageModel.setSendStatus(MessageSendStatus.MessageSending);
        //发送消息的fromuser为客户端的当前用户
        messageModel.setFrom_user(ClientManager.currentUserId);
        //发送消息的目标
        messageModel.setTo_user(toUser);
        //发送消息的类型
        messageModel.setChat_type(ChatType.chat);


        long currentTime = TimeUtils.getNowMills();
        //设置消息的出生和发射时间戳
        messageModel.setTimestamp(currentTime);
        messageModel.setSendTime(currentTime);

        //保存消息和会话到本地
        saveMessageAndConversationToDb(messageModel);
        //消息转JSON,发送给Socket
        String msgStr = JSON.toJSONString(messageModel);
        JSONObject message = null;
        try {
            message = new JSONObject(msgStr);

            String fileName = messageModel.getBodies().getFileName();
            if (fileName != null) { // 发送文件
                String filePath = "";
                switch (messageModel.getBodies().getType()) {
                    case img:
                        filePath = saveImage ? messageModel.getBodies().getOriginImagePath() : KUtil.imageSavePath() + fileName;
                        break;
                    case audio:
                        filePath = KUtil.audioSavePath() + fileName;
                        break;
                }
                //文件写入buffer数组
                FileInputStream inputStream = new FileInputStream(filePath);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);


                // 图片单独保存到本地
                if (messageModel.getBodies().getType() == MessageType.img && saveImage == true) {
                    String savePath = KUtil.imageSavePath() + messageModel.getBodies().getFileName();

                    File file = new File(savePath);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(buffer);
                    outputStream.close();
                }

                //文件字节数组封装成字节数组,构造json对象丢给Socket
                JSONObject object = message.getJSONObject("bodies");
                object.put("fileData", buffer);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //发送消息
        if (message != null) {

            SocketManager.socket.emit("chat", message, new Ack() {
                @Override
                public void call(Object... args) {
                    //服务器收到消息,转发给touser后，服务器还会回发更新后的消息
                    JSONObject successMsg = (JSONObject) args[0];
                    //消息模型设置为发送成功
                    messageModel.setSendStatus(MessageSendStatus.MessageSendSuccess);
                    try {
                        //消息id由服务器计算返回
                        messageModel.setMsg_id(successMsg.getString("msg_id"));
                        //更新消息成功发送的时间戳
                        messageModel.setTimestamp(successMsg.getLong("timestamp"));


                        // 发送定位||图片||语音文件成功，拿到截图地址
                        if (messageModel.getBodies().getType() == MessageType.loc
                                || messageModel.getBodies().getType() == MessageType.audio
                                || messageModel.getBodies().getType() == MessageType.img) {

                            String path = successMsg.getJSONObject("bodies").getString("fileRemotePath");
                            Log.e("KAKA", "发送图片或者录音成功" + path);
                            messageModel.getBodies().setFileRemotePath(path);
                        }

                        // 消息发送成功后更新数据库
                        DbManager.getsInstance().updateMessage(messageModel);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    callBack.sendStatus(messageModel);
                }
            });
        }

        return messageModel;
    }

    /**
     * 有新消息进来,添加消息，添加或者更新会话
     * @param messageModel
     */
    private static void saveMessageAndConversationToDb(MessageModel messageModel) {

        // 保存消息
        DbManager.getsInstance().insertMessage(messageModel);

        // 保存会话
        DbManager.getsInstance().insertOrUpdateConversation(messageModel);
    }

    public interface SendStatusCallBack {

        void sendStatus(MessageModel messageModel);
    }
}
