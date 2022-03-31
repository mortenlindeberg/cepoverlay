package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.ControlInput;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.ControlRequest;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.PartialSnapshot;
import no.uio.ifi.dmms.cepoverlay.network.controlmessages.Snapshot;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstance;
import no.uio.ifi.dmms.cepoverlay.queryengine.Query;
import no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySink;
import no.uio.ifi.dmms.cepoverlay.queryengine.QuerySource;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import static no.uio.ifi.dmms.cepoverlay.queryengine.QueryControl.SNAPSHOT_FRAGMENT_LIMIT;

public class LargeSnapshotTest extends OverlayInstance {
    private static final Logger log = Logger.getLogger(LargeSnapshotTest.class.getName());
    private byte[] snapshotBuf;
    private int snapshotCounter = -1;
    private int size = 99999;
    private byte[] orgSnapshot = new byte[size];
    private boolean error = false;
    private ReentrantLock snapshotMutex = new ReentrantLock();

    public LargeSnapshotTest() {
        BasicConfigurator.configure();
        int j = 0;
        for (int i = 0; i < size; i++) {
            orgSnapshot[i] = (byte)j++;
            if (j == 10) j = 0;
        }
    }

    @Test
    public void largeSnapshotTest() throws InterruptedException {

        ControlInput controlInput = new ControlInput();
        controlInput.listenForData("localhost", 1080, this);

        Query q = new Query(1,"nothing", Arrays.asList(new QuerySource("nothing",1)), Arrays.asList(new QuerySink(1,"nothing",1,1)));
        QueryControl.sendSnapshot(new Snapshot(q, orgSnapshot, "localhost", 1080), "localhost", 1080);
        Thread.sleep(5000);
        Assert.assertFalse(error);

    }

    @Test
    public void largeUnorderedSnapshotTest() throws InterruptedException {

        ControlInput controlInput = new ControlInput();
        controlInput.listenForData("localhost", 1080, this);

        Query q = new Query(1,"nothing", Arrays.asList(new QuerySource("nothing",1)), Arrays.asList(new QuerySink(1,"nothing",1,1)));
        sendUnorderedSnapshot(new Snapshot(q, orgSnapshot, "localhost", 1080), "localhost", 1080);
        Thread.sleep(5000);
        Assert.assertFalse(error);
    }

    public static void sendUnorderedSnapshot(Snapshot snapshot, String address, int port) throws InterruptedException {
        log.debug("> Send query snapshot ("+snapshot.toString()+") to "+address+" "+port);
        if (snapshot.getSize() > SNAPSHOT_FRAGMENT_LIMIT) {

            int snapshotSize = snapshot.getSize();
            int remainingBytes = snapshotSize;
            int position = 0; /* The position in original snapshot that has been placed into fragments*/
            int fragments = (snapshotSize / SNAPSHOT_FRAGMENT_LIMIT);

            if ((fragments * SNAPSHOT_FRAGMENT_LIMIT) < snapshotSize) /* Handle the likely case where there are remains from above operation */
                fragments++;

            log.debug("> Oops.. Need to fragment snapshot of size "+snapshot.getSize()+" into "+fragments+" fragments");

            for (int i = 1; i <= fragments; i++) {
                int lastByte;

                if (SNAPSHOT_FRAGMENT_LIMIT > remainingBytes)
                    lastByte = position + remainingBytes;
                else
                    lastByte = position+SNAPSHOT_FRAGMENT_LIMIT;


                PartialSnapshot partialSnapshot = new PartialSnapshot(snapshot.getQuery(), i, fragments, Arrays.copyOfRange(snapshot.getSnapshot(), position, lastByte), snapshot.getMasterAddress(), snapshot.getMasterPort());
                ControlRequest request = new ControlRequest(partialSnapshot);
                if (i == 2 || i == 7) {
                    sendDelayed(request, address, port, i, fragments, position);
                }
                else {
                    QueryControl.sendControl(request, address, port);
                    log.debug("> Sent: "+i+" of "+fragments+". Position: "+position+" to: "+lastByte+". Remaining fragments: " +(fragments-i)+" length: "+ partialSnapshot.getPartialSnapshot().length+" ");
                }

                int sent = lastByte - position;
                remainingBytes = remainingBytes - sent;
                position = lastByte;

            }
            log.debug("> ..done");
        }
    }

    private static void sendDelayed(ControlRequest request, String address, int port, int i, int fragments, int position) throws InterruptedException {
        Runnable run = () -> {
            try {
                Thread.sleep(1000);
                QueryControl.sendControl(request, address, port);
                log.debug("> Sent: "+i+" of "+fragments+". Position: "+position+".");
            } catch (InterruptedException e) {
                System.out.println(" interrupted");
            }
        };
        new Thread(run).start();

    }

    @Override
    public void handleControlRequest(ControlRequest request) throws InterruptedException {
        switch (request.getType()) {
            case QueryControl.CONTROL_PARTIAL_SNAPSHOT:
                PartialSnapshot ps = request.getQueryPartialSnapshot();
                handlePartialSnapshot(ps);
                break;
            default:
                log.error("Received unknown command to snapshot test.. not a partial snapshot??");
                break;
        }
    }


    private void handlePartialSnapshot(PartialSnapshot ps) {
        snapshotMutex.lock();
        snapshotCounter++;
        if (snapshotBuf == null) {
            snapshotBuf = new byte[QueryControl.SNAPSHOT_FRAGMENT_LIMIT*ps.getAll()]; // Remember, this means we need to trim at the end..
        }

        int dstPos = QueryControl.SNAPSHOT_FRAGMENT_LIMIT*(ps.getNum()-1);
        log.debug(">Handling partial snapshot! " + ps.getNum() + " of " + ps.getAll() + " received " + ps.getPartialSnapshot().length + "  bytes. Counter:  " + snapshotCounter +" writing from " +dstPos);

        System.arraycopy(ps.getPartialSnapshot(), 0, snapshotBuf, dstPos, ps.getPartialSnapshot().length);

        /* If we receive the last one, trim the buffer */
        if (ps.getAll() == ps.getNum()) {

            int newLength = (ps.getNum()*QueryControl.SNAPSHOT_FRAGMENT_LIMIT) - (QueryControl.SNAPSHOT_FRAGMENT_LIMIT - ps.getPartialSnapshot().length);
            log.debug("> Trimming now size "+ newLength);
            byte[] trimmedBuf = new byte[newLength];
            System.arraycopy(snapshotBuf, 0, trimmedBuf, 0, trimmedBuf.length);
            snapshotBuf = trimmedBuf.clone();
        }

        if (snapshotCounter == ps.getAll()) {
            compare(snapshotBuf, orgSnapshot);
            snapshotBuf = null;
            snapshotCounter = 0;
        }

        snapshotMutex.unlock();
    }

    private void compare(byte[] snapshotBuf, byte[] orgSnapshot) {
        int len = snapshotBuf.length;
        if (orgSnapshot.length != len) {
            log.error("Length Not the same: " +orgSnapshot.length +" "+ len);
            error = true;
        }

        for (int i = 0; i < len; i++)
            if (snapshotBuf[i] != orgSnapshot[i]) {
                log.error("Not the same: " + snapshotBuf[i] + " != " + orgSnapshot[i]);
                error = true;
            }
    }
}
