package no.uio.ifi.dmms.cepoverlay.network.datamessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import no.uio.ifi.dmms.cepoverlay.queryengine.Tuple;
import org.apache.commons.lang3.SerializationUtils;

import java.util.List;

public class DataRequestDecoder extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 9) /* If we cannot read the full length of the two integers, then wait..*/
            return;

        int streamId = in.readInt();
        int objLen = in.readInt();

        if (objLen > in.readableBytes()) {
            in.resetReaderIndex();
            return;
        }
        byte[] b = new byte[objLen];
        in.readBytes(b);

        Object[] tuplePayload = SerializationUtils.deserialize(b);
        out.add(new Tuple(streamId, tuplePayload));

        in.markReaderIndex();
    }
}
