package com.gupar.p2pchat.imservice.event;

/**
 * @author : ycj on 15-11-17.
 * @email : gupar@qq.com.
 *
 */
public enum SocketEvent {
    /**登陆之前的动作*/
    NONE,

    /**请求登陆的过程*/
    CONNECTING_MSG_SERVER,
    CONNECT_MSG_SERVER_SUCCESS,
    CONNECT_MSG_SERVER_FAILED,
    MSG_SERVER_DISCONNECTED    //channel disconnect 会触发，再应用开启内，要重连【可能是服务端、客户端断掉】
}
