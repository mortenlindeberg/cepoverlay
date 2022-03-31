package no.uio.ifi.dmms.cepoverlay.network.datamessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.commons.lang3.SerializationUtils;

public class DataRequestEncoder extends MessageToByteEncoder<DataRequest> {

    @Override
    protected void encode(ChannelHandlerContext ctx, DataRequest msg, ByteBuf out) {
        out.writeInt(msg.getStreamId());
        byte[] data = SerializationUtils.serialize(msg.getTuple());
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
