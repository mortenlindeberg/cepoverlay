package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequest;
import no.uio.ifi.dmms.cepoverlay.network.datamessages.DataRequestEncoder;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class LateArrivalHandler {
    private static final Logger log = Logger.getLogger(LateArrivalHandler.class.getName());

    private ConcurrentHashMap<Integer, QuerySink> lateArrivalMap;
    private ConcurrentHashMap<QuerySink, Channel> channelMap;

    private int count = 0;
    private int handles = 0;

    public LateArrivalHandler() {
        lateArrivalMap = new ConcurrentHashMap<>();
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel createChannel(String address, int port) {
        Channel channel = null;
        Bootstrap bootstrap;
        EventLoopGroup group;

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
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
        return channel;
    }

    public void send(Channel channel, Tuple tuple) {
        //log.debug(System.currentTimeMillis()+ " -> Late send " +tuple.getTuple()[0]+" "+tuple.getTuple()[1]);
        DataRequest request = new DataRequest();
        request.setStreamId(tuple.getStreamId());
        request.setTuple(tuple.getTuple());
        channel.writeAndFlush(request);
    }

    public void updateLateArrivalMap(int streamId, QuerySink sink) {
        lateArrivalMap.put(streamId, sink);
    }

    public void removeSink(int streamId) {
        lateArrivalMap.remove(streamId);
    }

    public boolean handle(Tuple tuple) {
        handles++;
        QuerySink sink = lateArrivalMap.get(tuple.getStreamId());
        if (sink != null) {
            //log.debug(System.currentTimeMillis() + " -> Handle late arrival: " + tuple.getStreamId() +" " +tuple.getTuple()[0]);
            Channel channel = getChannel(sink);
            send(channel, tuple);
            count++;
            return true;
        }
//        else
  //          log.debug(System.currentTimeMillis() + " -> Not late arrival: " + tuple.getStreamId() +" " +tuple.getTuple()[0]);

        return false;
    }

    public Channel getChannel(QuerySink sink) {
        for (QuerySink i : channelMap.keySet()) {
            if (i.getAddress().equals(sink.getAddress()) &&
                i.getPort() == sink.getPort() &&
                i.getStreamId() == sink.getStreamId())
                return channelMap.get(i);
        }

        Channel out = createChannel(sink.getAddress(), sink.getPort());
        channelMap.put(sink, out);
        return out;
    }

    public String getState() {
        return +count + " of " + handles;
    }
}