package no.uio.ifi.dmms.cepoverlay.network;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstance;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlRequest;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlResponse;
import org.apache.log4j.Logger;


public class ControlInputHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = Logger.getLogger(ControlInputHandler.class.getName());

    private OverlayInstance node;

    public ControlInputHandler(OverlayInstance node) {
        this.node = node;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ControlRequest request = (ControlRequest) msg;
        try {
            node.handleControlRequest(request);
        } catch (InterruptedException e) {
            log.error("Exception when reading from ControlChannel: "+e.getMessage());
        }

        ControlResponse response = new ControlResponse(request.getType());
        response.setStatus(request.getType());
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }
}
