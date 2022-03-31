package no.uio.ifi.dmms.cepoverlay.queryengine;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;

public class EngineWarmUp {

    private static String siddhiApp = "define stream StockEventStream (symbol string, price float, volume long); " +
            " " +
            "@info(name = 'query1') " +
            "from StockEventStream#window.time(5 sec)  " +
            "select symbol, sum(price) as price, sum(volume) as volume " +
            "group by symbol " +
            "insert into AggregateStockStream ;";

    public void warmUp() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        siddhiAppRuntime.addCallback("AggregateStockStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                // Do nothing
            }
        });

        //Start SiddhiApp runtime
        siddhiAppRuntime.start();
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StockEventStream");
        for (int i = 0; i < 1000; i++){
            //Sending events to Siddhi
            inputHandler.send(new Object[]{"IBM", 100f, 100L});
            inputHandler.send(new Object[]{"IBM", 200f, 300L});
            inputHandler.send(new Object[]{"WSO2", 60f, 200L});
            inputHandler.send(new Object[]{"WSO2", 70f, 400L});
            inputHandler.send(new Object[]{"GOOG", 50f, 30L});
            inputHandler.send(new Object[]{"IBM", 200f, 400L});
            inputHandler.send(new Object[]{"WSO2", 70f, 50L});
            inputHandler.send(new Object[]{"WSO2", 80f, 400L});
            inputHandler.send(new Object[]{"GOOG", 60f, 30L});
            Thread.sleep(10);
        }

        byte[] snapshot = siddhiAppRuntime.snapshot();
        try {
            siddhiAppRuntime.restore(snapshot);
        } catch (CannotRestoreSiddhiAppStateException e) {
            e.printStackTrace();
        }

        //Shutdown SiddhiApp runtime
        siddhiAppRuntime.shutdown();

        //Shutdown Siddhi
        siddhiManager.shutdown();
    }

}
