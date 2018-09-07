package com.gupar.p2pchat.utils;

import com.gupar.p2pchat.R;

import com.gupar.p2pchat.imservice.event.LoginEvent;
import com.gupar.p2pchat.imservice.event.SocketEvent;


public class IMUIHelper {



    // 根据event 展示提醒文案
    public static int getLoginErrorTip(LoginEvent event) {
        switch (event) {
            case LOGIN_AUTH_FAILED:
                return R.string.login_error_general_failed;
            case LOGIN_INNER_FAILED:
                return R.string.login_error_unexpected;
            default :
                return  R.string.login_error_unexpected;
        }
    }

    public static int getSocketErrorTip(SocketEvent event) {
        switch (event) {
            case CONNECT_MSG_SERVER_FAILED :
                return R.string.connect_server_failed;
            default :
                return  R.string.login_error_unexpected;
        }
    }
}
