package no.uio.ifi.dmms.cepoverlay.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequestDecoder;
import org.apache.log4j.Logger;

import java.util.concurrent.PriorityBlockingQueue;

public class DataInput {
    private static final Logger log = Logger.getLogger(DataInput.class.getName());

    private ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    public void listenForData(String host, int port, PriorityBlockingQueue tupleQueue) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast(new DataRequestDecoder(), new DataInputHandler(tupleQueue));
                    }
                });
        try {
            channelFuture = bootstrap.bind(host, port).sync();
            log.info("-> Overlay node listening for incoming data: " +host+" "+port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        log.debug("-> Data Input closing down..");
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
