package com.qicode.kakaxicm.kchat.manager;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.enums.MessageEventType;
import com.qicode.kakaxicm.kchat.enums.SocketConnectStatus;
import com.qicode.kakaxicm.kchat.model.MessageEvent;
import com.qicode.kakaxicm.kchat.model.MessageModel;
import com.qicode.kakaxicm.kchat.util.KLog;
import com.qicode.kakaxicm.kchat.util.KUtil;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by chenming on 2018/9/20
 */
public class SocketManager {

    public static Socket socket;

    public static SocketConnectStatus connectStatus = SocketConnectStatus.SocketDisconnected;

    private static SocketCallBack callBack;

    public static abstract class SocketCallBack {

        public abstract void success();

        public abstract void fail();
    }

    /**
     * 构造socket，并发起连接
     * @param context
     * @param token
     * @param callBack
     */
    public static void connect(Context context, String token, final SocketCallBack callBack) {
        SocketManager.callBack = callBack;
        if (socket != null) {
            socket = null;
        }
        initSocket(token);

        socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                callBack.success();
            }
        });
        socket.connect();


        addHandles(context);

    }

    /**
     * 配置socket
     *
     * @param token
     */
    private static void initSocket(String token) {
        IO.Options opts = new IO.Options();
        opts.forceNew = false;
        opts.reconnection = true;
        opts.reconnectionDelay = 2000;      //延迟
        opts.reconnectionDelayMax = 6000;
        opts.reconnectionAttempts = -1;
        opts.timeout = 6000;
        opts.query = "auth_token=" + token;
        try {
            socket = IO.socket(UrlConstant.baseUrl, opts);
        } catch (Exception e) {
        }
    }

    /**
     * 处理服务端发给客户端消息，这里是各种通知的源头,通过EB转发消息
     * @param context
     */
    private static void addHandles(final Context context) {
        //连接状态的消息监听
        /**
         * 断开连接
         */
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                KLog.i("连接断开");
                connectStatus = SocketConnectStatus.SocketDisconnected;
                EventBus.getDefault().post(new MessageEvent(MessageEventType.EventConnectStatus, SocketConnectStatus.SocketDisconnected));
            }
        });

        /**
         * 连接中
         */
        socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                KLog.i("连接中");
                connectStatus = SocketConnectStatus.SocketConnecting;
                EventBus.getDefault().post(new MessageEvent(MessageEventType.EventConnectStatus, SocketConnectStatus.SocketConnecting));
            }
        });

        /**
         * 连接失败
         */
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                KLog.i("连接失败");
                connectStatus = SocketConnectStatus.SocketConnectError;
                EventBus.getDefault().post(new MessageEvent(MessageEventType.EventConnectStatus, SocketConnectStatus.SocketConnectError));
            }
        });
        /**
         * 连接成功
         */
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                KLog.i("连接成功");
                connectStatus = SocketConnectStatus.SocketConnected;
                EventBus.getDefault().post(new MessageEvent(MessageEventType.EventConnectStatus, SocketConnectStatus.SocketConnected));
            }
        });


        // 收到信消息
        socket.on("chat", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                KLog.i("收到消息");
                Ack ack = (Ack) args[1];
                ack.call("我已收到消息");
                JSONObject msg = (JSONObject) args[0];


                byte[] file = null;
                try {
                    JSONObject msgBody = msg.getJSONObject("bodies");

                    file = (byte[]) msgBody.get("fileData");

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final MessageModel message = JSON.parseObject(msg.toString(), MessageModel.class);

                if (file != null) {

                    String fileName = message.getBodies().getFileName();
                    String savePath = "";
                    switch (message.getBodies().getType()) {
                        case img:
                            savePath = KUtil.imageSavePath() + fileName;
                            break;
                        case audio:
                            savePath = KUtil.audioSavePath() + fileName;
                            break;
                    }
                    File imageFile = new File(savePath);
                    try {
                        FileOutputStream outputStream = new FileOutputStream(imageFile);
                        outputStream.write(file);
                        outputStream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                // 消息保存数据库
                DbManager.getsInstance().insertMessage(message);

                // 更新会话
                DbManager.getsInstance().insertOrUpdateConversation(message);

                // 发送事件
                MessageEvent event = new MessageEvent(MessageEventType.EventMessage, message);
                EventBus.getDefault().post(event);
            }
        });

        //TODO 视频电话请求监听


        // 用户上线
        socket.on("onLine", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                JSONObject msg = (JSONObject) args[0];
                try {
                    MessageEvent event = new MessageEvent(MessageEventType.EventUserOnline, msg.getString("user"));
                    EventBus.getDefault().post(event);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        // 用户下线
        socket.on("offLine", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject msg = (JSONObject) args[0];
                try {
                    MessageEvent event = new MessageEvent(MessageEventType.EventUserOffLine, msg.getString("user"));
                    EventBus.getDefault().post(event);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // 连接状态改变
        socket.on("statusChange", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                KLog.i("连接状态确实改变了......");
            }
        });
    }
}
