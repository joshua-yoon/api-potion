package com.tmoncorp.mobile.util.spring.clientinfo;

import com.tmoncorp.mobile.util.common.clientinfo.ClientInfo;
import com.tmoncorp.mobile.util.common.clientinfo.ClientInfoProvider;

public class ClientInfoService {

    private ClientInfoService() {
        throw new AssertionError("static utility class");
    }

    public static ClientInfo getInfo() {
        return ClientInfoProvider.getInfo();
    }

    public static void setInfo(ClientInfo info) {
        ClientInfoProvider.setInfo(info);
    }

    public static void clean() {
        ClientInfoProvider.clean();
    }

}