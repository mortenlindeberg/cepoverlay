package no.uio.ifi.dmms.cepoverlay.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstance;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlRequestDecoder;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlResponseEncoder;
import no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread;
import org.apache.log4j.Logger;


public class ControlInput {
    private static final Logger log = Logger.getLogger(ControlInput.class.getName());

    private ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    public void listenForData(String host, int port, OverlayInstance node) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast(new ControlRequestDecoder(),
                                new ControlResponseEncoder(),
                                new ControlInputHandler(node));
                    }
                });
        try {
            channelFuture = bootstrap.bind(host, port).sync();
            log.info("-> Overlay node listening for incoming control: " +host+" "+port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* This is the function to handle control data for sources not the overlay instances.. */
    public void listenForData(String host, int port, SimpleSourceThread source) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast(new ControlRequestDecoder(),
                                new ControlResponseEncoder(),
                                new SourceControlInputHandler(source));
                    }
                });
        try {
            channelFuture = bootstrap.bind(host, port).sync();
            log.info("-> Overlay node listening for incoming control: " +host+" "+port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        log.debug("-> Control input closing down..");
        channelFuture.channel().close();
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
