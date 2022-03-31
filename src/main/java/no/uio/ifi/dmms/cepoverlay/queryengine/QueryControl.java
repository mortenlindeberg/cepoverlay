package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import no.uio.ifi.dmms.cepoverlay.network.ControlInputHandler;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.*;
import org.apache.log4j.Logger;

import java.util.Arrays;

public class QueryControl {
    public static final int CONTROL_NEW_QUERY = 1;
    public static final int CONTROL_MIGRATE = 2;
    public static final int CONTROL_SNAPSHOT = 3;
    public static final int CONTROL_QUERY_STOP = 4;
    public static final int CONTROL_OVERLAY_STOP = 5;
    public static final int CONTROL_REDIRECT = 6;
    public static final int SINK_TYPE_FORWARD = 7;
    public static final int SINK_TYPE_WRITE = 8;
    public static final int SINK_TYPE_PRED_ADAPT = 9;
    public static final int SINK_TYPE_STAT_ADAPT = 10;
    public static final int SOURCE_REDIRECT = 11;
    public static final int ABORT_LATE_ARRIVAL = 12;
    public static final int CONTROL_PARTIAL_SNAPSHOT = 13;
    public static final int RATE_UPDATE = 14;
    public static final int QUERY_MIGRATION_FINISHED = 15;
    public static final int WINDOW_AWARE_QUERY_MIGRATE = 16;
    public static final int SINK_TYPE_FORWARD_WITH_FLUSH_AWARENESS = 17;

    private static final Logger log = Logger.getLogger(QueryControl.class.getName());
    public static final int SNAPSHOT_FRAGMENT_LIMIT = 10000; // This is the limit that works

    public static void sendQuery(Query q, String address, int port) throws InterruptedException {
        ControlRequest request = new ControlRequest(q);
        sendControl(request, address, port);
    }

    public static void sendSnapshot(Snapshot snapshot, String address, int port) throws InterruptedException {
        log.debug("> Send query snapshot of "+snapshot.getSnapshot().length+" bytes to "+address+" "+port);
        if (snapshot.getSize() > SNAPSHOT_FRAGMENT_LIMIT) {

            int snapshotSize = snapshot.getSize();
            int remainingBytes = snapshotSize;
            int position = 0; /* The position in original snapshot that has been placed into fragments*/
            int fragments = (snapshotSize / SNAPSHOT_FRAGMENT_LIMIT);

            if ((fragments * SNAPSHOT_FRAGMENT_LIMIT) < snapshotSize) /* Handle the likely case where there are remains from above operation */
                fragments++;

            //log.debug("> Oops.. Need to fragment snapshot of size "+snapshot.getSize()+" into "+fragments+" fragments");

            for (int i = 1; i <= fragments; i++) {
                int lastByte;

                if (SNAPSHOT_FRAGMENT_LIMIT > remainingBytes)
                    lastByte = position + remainingBytes;
                else
                    lastByte = position+SNAPSHOT_FRAGMENT_LIMIT;


                PartialSnapshot partialSnapshot = new PartialSnapshot(snapshot.getQuery(), i, fragments, Arrays.copyOfRange(snapshot.getSnapshot(), position, lastByte), snapshot.getMasterAddress(), snapshot.getMasterPort());
                ControlRequest request = new ControlRequest(partialSnapshot);
                sendControl(request, address, port);

                int sent = lastByte - position;
                remainingBytes = remainingBytes - sent;
                position = lastByte;
                //log.debug("> Sent: "+i+" of "+fragments+". Position: "+position+" to: "+lastByte+". Remaining fragments: " +(fragments-i)+" length: "+ partialSnapshot.getPartialSnapshot().length+" ");
            }
            //log.debug("> ..done");
        }
        else {
            //log.debug("> Oops.. No need to fragment the snapshot "+snapshot.getSize()+" into fragments");
            ControlRequest request = new ControlRequest(snapshot);
            sendControl(request, address, port);
        }
    }

    public static void sendQueryMigrate(QueryMigrate queryMigrate, String address, int port) throws InterruptedException {
        //log.debug("> Send query migrate ("+queryMigrate.toString()+") to "+address+" "+port);
        ControlRequest request = new ControlRequest(queryMigrate);
        sendControl(request, address, port);
    }
    public static void sendQueryStop(QueryStop queryStop, String address, int port) throws InterruptedException {
        //log.debug("> Send query stop to " +address+" "+port);
        ControlRequest request = new ControlRequest(queryStop);
        sendControl(request, address, port);
    }

    public static void sendQueryRedirect(QueryRedirect queryRedirect, String address, int port) throws InterruptedException {
        //log.debug("> Send query redirect ("+queryRedirect.toString()+") to "+address+" "+port);
        ControlRequest request = new ControlRequest(queryRedirect);
        sendControl(request, address, port);
    }

    public static void sendOverlayStop(String address, int port) throws InterruptedException {
        log.debug("> Sending overlay stop");
        ControlRequest request = new ControlRequest(new OverlayStop(port));
        sendControl(request, address, port);
    }


    public static void sendSourceRedirect(SourceRedirect sourceRedirect, String address, int port) throws InterruptedException {
        //log.debug("> Sending source redirect");
        ControlRequest request = new ControlRequest(sourceRedirect);
        sendControl(request, address, port);
    }

    public static void sendAbortLateArrival(AbortLateArrival abortRequest, String address, int port) throws InterruptedException {
        log.debug("> Sending abort late arrival "+address+" "+port+" "+abortRequest.getStreamId());
        ControlRequest request = new ControlRequest(abortRequest);
        sendControl(request, address, port);
    }

    public static void sendRateUpdate(RateUpdate rateUpdate, String address, int port) throws InterruptedException {
        //log.debug("> Sending rate update: "+rateUpdate.isMode());
        ControlRequest request = new ControlRequest(rateUpdate);
        sendControl(request, address, port);
    }

    public static void sendQueryMigrationFinished(QueryMigrationFinished queryMigrationFinished, String address, int port) throws InterruptedException {
        log.debug("> Sending query migration finished to "+address+" "+port);
        ControlRequest request = new ControlRequest(queryMigrationFinished);
        sendControl(request, address, port);
    }

    public static void sendWindowAwareQueryMigrate(WindowAwareQueryMigrate windowAwareQueryMigrate, String address, int port) throws InterruptedException {
        //log.debug("> Send window aware query migrate ("+windowAwareQueryMigrate.toString()+") to "+address+" "+port);
        ControlRequest request = new ControlRequest(windowAwareQueryMigrate);
        sendControl(request, address, port);
    }

    /* This code is heavily influenced by how Siddhi uses Netty ;) */
    public static void sendControl(ControlRequest request, String address, int port) throws InterruptedException {
        Channel channel;
        Bootstrap bootstrap;
        EventLoopGroup group;
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst(new ControlRequestEncoder(), new ControlResponseDecoder(), new ControlInputHandler(null));
            }
        });

        channel = bootstrap.connect(address, port).sync().channel();
        channel.writeAndFlush(request);

        channel.close();
        channel.closeFuture().sync();
        channel.disconnect();

        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
