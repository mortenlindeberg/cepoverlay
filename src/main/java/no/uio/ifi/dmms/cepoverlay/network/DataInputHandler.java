package no.uio.ifi.dmms.cepoverlay.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.util.concurrent.PriorityBlockingQueue;

public class DataInputHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = Logger.getLogger(DataInputHandler.class.getName());

    private PriorityBlockingQueue tupleQueue;

    public DataInputHandler(PriorityBlockingQueue tupleQueue) {
        this.tupleQueue = tupleQueue;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //log.debug(System.currentTimeMillis()+ " -> Channel read " +((Tuple)msg).getTuple()[0]+" "+((Tuple)msg).getTuple()[1]);
        tupleQueue.add(msg);
    }
}