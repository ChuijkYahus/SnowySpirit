package net.mehvahdjukaar.snowyspirit.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;

public class ModNetworking {

    public static void init() {
        NetworkHelper.addNetworkRegistration(event -> {
                    event.registerServerBound(ServerBoundUpdateSledStateMessage.CODEC);
                    event.registerClientBound(ClientBoundMarkPosForRebuildMessage.CODEC);
                },
                4);
    }

}