package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ControlResponseEncoder extends MessageToByteEncoder<ControlResponse> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ControlResponse msg, ByteBuf out) {

    }
}