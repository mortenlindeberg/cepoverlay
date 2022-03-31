package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.siddhi.core.event.Event;
import io.siddhi.core.stream.output.StreamCallback;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequest;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequestEncoder;
import org.apache.log4j.Logger;


public class QueryCallbackSend extends StreamCallback {
    private static final Logger log = Logger.getLogger(QueryCallbackSend.class.getName());
    private int streamId;
    private String address;
    private int port;

    private Channel channel;
    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public QueryCallbackSend(int streamId, String address, int port) {
        this.streamId = streamId;
        this.address = address;
        this.port = port;

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addFirst(new DataRequestEncoder());
                    }
                });
        try {
            channel = bootstrap.connect(address, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void receive(Event[] events) {
        //log.debug(System.currentTimeMillis()+ " -> Forward send " +events[0].getData()[0]+" "+events[0].getData()[1]+" events.length:" +events.length);
        DataRequest request = new DataRequest();
        request.setStreamId(streamId);
        request.setTuple(events[0].getData());
        channel.writeAndFlush(request);
    }

    public synchronized void redirect(int streamId, String address, int port) {
        this.streamId = streamId;
        this.address = address;
        this.port = port;
        channel.close();
        try {
            channel = bootstrap.connect(address, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        group.shutdownGracefully();
    }

    @Override
    public String toString() {
        return "QueryCallbackSend{" +
                "streamId=" + streamId +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
