package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.queryengine.Tuple;
import org.junit.Test;

import java.util.concurrent.PriorityBlockingQueue;

import static org.junit.Assert.assertEquals;

public class RingBufferTest {

    @Test
    public void testRingBuffer() {
        PriorityBlockingQueue<Tuple> ringBuffer = new PriorityBlockingQueue<Tuple>(100);


        Tuple t1 = new Tuple(1, new Object[]{(long) 1});
        Tuple t2 = new Tuple(1, new Object[]{(long) 2});
        Tuple t3 = new Tuple(1, new Object[]{(long) 3});
        Tuple t4 = new Tuple(1, new Object[]{(long) 4});

        ringBuffer.add(t1);
        ringBuffer.add(t4);
        ringBuffer.add(t2);
        ringBuffer.add(t3);

        Tuple o1 = ringBuffer.poll();
        Tuple o2 = ringBuffer.poll();
        System.out.println(ringBuffer);
        assertEquals(o2,t2);
    }


}
