package com.irtimaled.bbor.mixin.network.play.server;

import com.irtimaled.bbor.common.EventBus;
import com.irtimaled.bbor.common.messages.AddBoundingBox;
import com.irtimaled.bbor.common.messages.InitializeClient;
import com.irtimaled.bbor.common.messages.PayloadReader;
import com.irtimaled.bbor.common.messages.SubscribeToServer;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SCustomPayloadPlayPacket.class)
public abstract class MixinSCustomPayloadPlayPacket {
    @Shadow
    private ResourceLocation channel;

    @Redirect(method = "processPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/play/IClientPlayNetHandler;handleCustomPayload(Lnet/minecraft/network/play/server/SCustomPayloadPlayPacket;)V"))
    private void processPacket(IClientPlayNetHandler netHandlerPlayClient, SCustomPayloadPlayPacket packet) {
        String channelName = channel.toString();
        if (channelName.startsWith("bbor:")) {
            PacketBuffer data = null;
            try {
                data = packet.getBufferData();
                PayloadReader reader = new PayloadReader(data);
                switch (channelName) {
                    case InitializeClient.NAME: {
                        EventBus.publish(InitializeClient.getEvent(reader));
                        ((ClientPlayNetHandler) netHandlerPlayClient).sendPacket(SubscribeToServer.getPayload().build());
                        break;
                    }
                    case AddBoundingBox.NAME:
                    case AddBoundingBox.LEGACY: {
                        EventBus.publish(AddBoundingBox.getEvent(reader, channelName));
                        break;
                    }
                }
            } finally {
                if (data != null)
                    data.release();
            }
        } else {
            netHandlerPlayClient.handleCustomPayload(packet);
        }
    }
}
