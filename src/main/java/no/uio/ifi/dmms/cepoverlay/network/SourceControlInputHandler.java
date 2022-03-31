package no.uio.ifi.dmms.cepoverlay.network;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlRequest;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlResponse;
import no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread;

public class SourceControlInputHandler extends ChannelInboundHandlerAdapter {

    private SimpleSourceThread source;

    public SourceControlInputHandler(SimpleSourceThread source) {
        this.source = source;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ControlRequest request = (ControlRequest) msg;

        source.handleControlRequest(request);

        ControlResponse response = new ControlResponse(request.getType());
        response.setStatus(request.getType());
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }
}
