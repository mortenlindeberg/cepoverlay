package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ControlRequestEncoder extends MessageToByteEncoder<ControlRequest> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ControlRequest msg, ByteBuf out) throws Exception {
        //System.out.println(System.currentTimeMillis()+" >Encoding .."+msg.getType()+" " +msg.getPayload().length);
        out.writeInt(msg.getType());
        byte[] payload = msg.getPayload();
        out.writeInt(payload.length);
        out.writeBytes(payload);
        //System.out.println(System.currentTimeMillis()+" >Encoded..");
    }
}
