package no.uio.ifi.dmms.cepoverlay.network.controlmessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ControlRequestDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int type = in.readInt();
        int len = in.readInt();
        //System.out.println(System.currentTimeMillis() +">Decoding "+type+" "+len+" "+in.readableBytes());

        byte[] payload = new byte[len];

        if (len > in.readableBytes()) {
            in.resetReaderIndex(); /* Wait, entire message is not read yet*/
            return;
        }

        in.readBytes(payload);
        out.add(new ControlRequest(type, payload));
        //System.out.println(System.currentTimeMillis()+"> Decoded..");
    }
}
