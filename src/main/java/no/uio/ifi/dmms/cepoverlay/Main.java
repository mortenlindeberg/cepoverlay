package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.network.topology.Instance;
import no.uio.ifi.dmms.cepoverlay.network.topology.RouteHelper;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstanceThread;
import no.uio.ifi.dmms.cepoverlay.prediction.*;
import no.uio.ifi.dmms.cepoverlay.queries.ConfigCreator;
import no.uio.ifi.dmms.cepoverlay.queries.ConfigReader;
import no.uio.ifi.dmms.cepoverlay.queries.Runners;
import no.uio.ifi.dmms.cepoverlay.source.*;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final String CONFIG_PROPERTIES = "config.properties";
    public static final double LIMIT = 20;
    public static final double LOAD_LIMIT = 62000;
    public static final double HEART_LIMIT = 35.2;
    public static final double PAMAP_LIMIT_HR = 100;
    public static final double PAMAP_LIMIT_TEMP = 32;
    public static final int ACTIVITY_ID = 0;
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static final String START_FILENAME = "start.res";
    public static final String ADAPT_FILENAME = "adapt.res";



    public static void main(String[] args) throws ParseException, InterruptedException {
        BasicConfigurator.configure();
        log.info("-> Starting..");

        Options options = new Options();
        options.addOption("name", true, "Instance name");

        options.addOption("la", true, "Local address");
        options.addOption("lp", true, "Local port");

        options.addOption("ra", true, "Remote address");
        options.addOption("rp", true, "Remote port");

        options.addOption("sid", true, "Stream ID");
        options.addOption("sleep", true, "Sleep time");
        options.addOption("noise", true, "Integer defining noise from Gaussian distribution");
        options.addOption("prolong", true, "Prolongment (ms) of upper mode");
        options.addOption("frequency", true, "Frequency of oscillations in signal (per minute)");


        options.addOption("source", false, "Run source [FireSource] on this node, argument defines sleep time in between transmittion (effectively rate)");
        options.addOption("normal", false, "Normal node that just run the overlay (and is thereby ready to receive queries and data");
        options.addOption("master", false, "Initiate and master the experiment from this node, argument is an integer defining the strategy");
        options.addOption("strategy", true, "Migration strategy");

        options.addOption("nodes", true, "Comma separated ist of nodes in format [Node name]:[Type]:[IP address]:[Port]");
        options.addOption("links", true, "Comma separated list of links in format [Source]-[Destination]");

        options.addOption("router", true, "Run this argument to use the program as a tool to create route (in ns-3 and on each node). Parameter should specify the address range, e.x., 10.0.0.x, and x will be replaced by a counter");
        options.addOption("timestamp", false, "This option will write a timestamp to a file which is used to represent 0 point in time for experiment");
        options.addOption("duration", true, "The duration of the experiment in seconds");
        options.addOption("configurator", true, "Option to create a configuration file on the fly");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("source")) {
            log.debug("-> Source..");
            int localPort = Integer.parseInt(cmd.getOptionValue("lp"));
            String localAddress = cmd.getOptionValue("la");
            int remotePort = Integer.parseInt(cmd.getOptionValue("rp"));
            String remoteAddress = cmd.getOptionValue("ra");
            int streamId = Integer.parseInt(cmd.getOptionValue("sid"));
            int sleep = Integer.parseInt(cmd.getOptionValue("sleep"));
            log.debug("--> LocalAddress: "+localAddress+" LocalPort: "+localPort+" StreamId "+streamId+" RemoteAddress: "+remoteAddress+" RemotePort: "+remotePort);

            double noise;
            if (cmd.getOptionValue("noise") == null)
                noise = 0;
            else
                noise = Double.parseDouble(cmd.getOptionValue("noise"));

            long prolong;
            if (cmd.getOptionValue("prolong") == null)
                prolong = 0;
            else
                prolong = Long.parseLong(cmd.getOptionValue("prolong"));


            int strategy = 0;
            if (cmd.hasOption("strategy"))
                strategy = Integer.parseInt(cmd.getOptionValue("strategy"));

            log.debug("--> Noise: "+noise+" Prolong: "+prolong+" Strategy "+strategy);


            SimpleSourceThread t;

            if (streamId < 3) {
                t = new FireSource(localAddress, localPort, remoteAddress, remotePort, streamId, sleep, noise, prolong);
                if (strategy == 1) {
                    log.debug("> Adding reactive adaptation query!");
                    /* Execute the adaptation queries on the predictable "source node" */
                    t.addAdaptation(new StaticAdapterDirect(ADAPT_FILENAME));
                } else if (strategy == 2) {
                    log.debug("> Adding proactive adaptation query!");
                    /* Execute the adaptation queries on the predictable "source node" */
                    t.addAdaptation(new PredictiveAdapterDirect(ADAPT_FILENAME));
                }
                t.start();
                t.join();
            }
            else if (streamId < 5) {
                t = new OscilliatingFireSource(localAddress, localPort, remoteAddress, remotePort, streamId - 2, sleep, noise, Double.parseDouble(cmd.getOptionValue("frequency")));
                if (strategy == 1) {
                    log.debug("> Adding reactive adaptation query!");
                    /* Execute the adaptation queries on the predictable "source node" */
                    t.addAdaptation(new StaticAdapterDirect(ADAPT_FILENAME));
                }

                else if (strategy == 2) {
                    log.debug("> Adding proactive adaptation query!");
                    /* Execute the adaptation queries on the predictable "source node" */
                    t.addAdaptation(new PredictiveAdapterDirect(ADAPT_FILENAME));
                }
                t.start();
                t.join();
            }
            else if (streamId < 8) {
                log.debug("> TempLoadSource is now to be started (strategy=" + strategy + ", streamId=" + streamId + ")..");
                int numThreads = TempLoadSource.files.size();
                // .. oh wait, if this is the temp city stuff, we do it differently. Todo to clean this mess up!
                if (streamId == 6) {
                    ExecutorService taskExecutor = Executors.newFixedThreadPool(numThreads);
                    for (int i = 0; i < numThreads; i++) {
                        taskExecutor.execute(new TempLoadSource(localAddress, localPort + i - 1, remoteAddress, remotePort, 0, sleep, i));
                    }
                    taskExecutor.shutdown();
                    taskExecutor.awaitTermination(1, TimeUnit.HOURS);
                } else {
                    t = new TempLoadSource(localAddress, localPort, remoteAddress, remotePort, 2, sleep, 0);

                    if (strategy == 14 && streamId == 7)
                        t.addAdaptation(new PredictiveLoadAdapter(ADAPT_FILENAME, false));

                    t.start();
                    t.join();
                }
            }
            else { /* Everything in streamId > 8 is PAMAP experiments */
                int queryId = ConfigReader.readQueryId();
                double predLimit;
                int predIndex;
                if (queryId == 0 || queryId == 1){
                    predLimit = Main.PAMAP_LIMIT_TEMP;
                    predIndex = 2;
                }
                else {
                     predLimit = Main.PAMAP_LIMIT_HR;
                    predIndex = 1;
                }

                log.debug("> Activity is now to be started (strategy=" + strategy + ", streamId=" + streamId + ")..");

                t = new PAMAPSource(localAddress, localPort, remoteAddress, remotePort, streamId, sleep);

                if (strategy == 18 && streamId == 9) {
                    t.addAdaptation(new PredictiveHeartrateAdapter(ADAPT_FILENAME, false));
                }
                else if ((strategy == 24 || strategy == 26 || strategy == 28) && streamId % 2 == 0) {
                    boolean mode = true;
                    if (streamId == 11) // TODO: THIS WORK ONLY FOR QUERY 4!!!!!
                        mode = false;

                    String adaptMaster;
                    if (strategy == 24) adaptMaster = "10.0.0.15";
                    else adaptMaster = "10.0.0.27";
                    t.addAdaptation(new PAMAPLargeAdapter(ADAPT_FILENAME, mode, remoteAddress, adaptMaster, predLimit, predIndex));
                }


                else if ( (strategy == 27 || strategy == 29) && streamId % 2 == 0) {
                    boolean mode = true;
                    if (streamId == 11) // TODO: THIS WORK ONLY FOR QUERY 4!!!!!
                        mode = false;

                    String adaptMaster;
                    if (strategy == 24) adaptMaster = "10.0.0.15";
                    else adaptMaster = "10.0.0.27";
                    t.addAdaptation(new PAMAPLargeStaticAdapter(ADAPT_FILENAME, mode, remoteAddress, adaptMaster, predLimit, predIndex));
                }
                t.start();
                t.join();
            }

        } else if (cmd.hasOption("normal")) {
            String instanceName = cmd.getOptionValue("name");
            int localPort = Integer.parseInt(cmd.getOptionValue("lp"));
            String localAddress = cmd.getOptionValue("la");

            OverlayInstanceThread t = new OverlayInstanceThread(new Instance(instanceName, localAddress, localPort));
            t.start();
            t.join();
        } else if (cmd.hasOption("router")) {
            List<Instance> instances = null;

            instances = readInstances(cmd.getOptionValue("nodes"));
            log.debug(" -> Calculating routes for " + instances.size() + " nodes..");
            RouteHelper routeHelper = new RouteHelper(instances, cmd.getOptionValue("links"), cmd.getOptionValue("router"));

            // This will write the routes for the nodes (run in docker) to file
            for (Instance i : instances)
                for (Instance j : instances)
                    if (i != j)
                        routeHelper.createRouteForLinux(i, j);

            // This will write the routes for the routers to file
            for (Instance i : routeHelper.getRouters())
                for (Instance j : instances)
                    if (i != j)
                        routeHelper.createRouteForNs3(i, j);

            routeHelper.closeFiles();

        } else if (cmd.hasOption("timestamp")) {
            Runners.writeStartTime(START_FILENAME);
        } else if (cmd.hasOption("master")) {
            int queryId = ConfigReader.readQueryId();
            int strategy = Integer.parseInt(cmd.getOptionValue("strategy"));
            log.debug("> Master node instrumenting experiment with strategy: " + strategy);

            if (strategy < 10)
                Runners.syntheticQuery(cmd, strategy);
            else if (strategy < 15)
                Runners.loadTempQuery(cmd, strategy);
            else if (strategy < 18)
                Runners.activityQuery(cmd, strategy);
            else if (strategy < 20)
                Runners.windowAwarePAMAPQuery(cmd, strategy);
            else if (strategy == 20)
                Runners.largePAMAPQueryPX(cmd, queryId, "10.0.0.3");
            else if (strategy == 21)
                Runners.largePAMAPQueryPX(cmd, queryId, "10.0.0.6");
            else if (strategy == 22)
                Runners.largePAMAPQueryPX(cmd, queryId, "10.0.0.9");
            else if (strategy == 23)
                Runners.largePAMAPQueryPX(cmd, queryId, "10.0.0.12");
            else if (strategy == 24)
                Runners.largePAMAPQueryPX(cmd, queryId, "10.0.0.3");
            else if (strategy == 25)
                Runners.extraLargePAMAPQueryPX(cmd, queryId, "10.0.0.27");
            else if (strategy == 26)
                Runners.extraLargePAMAPQueryPX(cmd, queryId, "10.0.0.27");
            else if (strategy == 27)
                Runners.extraLargePAMAPQueryPX(cmd, queryId, "10.0.0.27");
            else if (strategy == 28)
                Runners.extraLargePAMAPQueryPXHardcode(cmd, queryId, "10.0.0.27");
            else
                log.error("Unknown strategy "+strategy);
        } else if (cmd.hasOption("configurator")) {
            try {
                ConfigCreator.createConfig(cmd.getOptionValue("configurator"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        log.info("-> Stopping..");
    }


    public static List<Instance> readInstances(String instanceString) {
        List<Instance> outList = new ArrayList<>();
        String[] instances = instanceString.split(",");
        for (String instance : instances) {
            String[] details = instance.split(":");
            outList.add(new Instance(details[0], details[1], Integer.parseInt(details[2]), Float.parseFloat(details[3]), details[4], 1));
        }
        return outList;
    }
}
