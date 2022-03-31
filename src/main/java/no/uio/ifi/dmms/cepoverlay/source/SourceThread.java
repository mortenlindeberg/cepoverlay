package no.uio.ifi.dmms.cepoverlay.source;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequest;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequestEncoder;
import org.apache.log4j.Logger;


public class SourceThread extends Thread {
    private static final Logger log = Logger.getLogger(SourceThread.class.getName());

    private String address;
    private int port;
    private Channel channel;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private int streamId;

    /* This code is heavily influenced by how Siddhi uses Netty ;) */
    public SourceThread(String address, int port, int streamId) {
        this.address = address;
        this.port = port;
        this.streamId = streamId;

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addFirst(new DataRequestEncoder());
                    }
                });
    }

    public void connect() {
        try {
            channel = bootstrap.connect(address, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void redirect(String address, int port) {
        log.debug("> Source redirecting to " +address+" "+ port);
        this.port = port;
        channel.flush();
        channel.close();
        try {
            channel = bootstrap.connect(address, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized ChannelFuture send(Object[] tuple) {
        //log.debug(System.currentTimeMillis()+ "-> Src send port +"+port+" " +tuple[0]+ " "+tuple[1]);
        DataRequest request = new DataRequest();
        request.setStreamId(streamId);
        request.setTuple(tuple);
        ChannelFuture cf = channel.writeAndFlush(request);
        return cf;
    }

    public void disconnect() {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
                channel.closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("Error closing connection to '" + address + " " + port + " " + e);
            }
            channel.disconnect();
            log.info("Disconnecting client to '" + address + " " + port);
        }
    }

    public void shutdown() {
        disconnect();
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("Stopping client to '" + address + " " + port);
    }
}

