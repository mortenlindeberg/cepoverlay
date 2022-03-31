package no.uio.ifi.dmms.cepoverlay;

import no.uio.ifi.dmms.cepoverlay.queries.ConfigCreator;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PropertiesTest {


    @Test
    public void automaticConfig() throws IOException {
        ConfigCreator.createConfig("learningWindowSize=20,edgePointsSize=3");
    }

    @Test
    public void writeConfig() throws IOException {
        //List<Integer> lWindow = Arrays.asList(6, 10, 20, 40);
        List<Integer> futures = Arrays.asList(20);
        //List<Integer> slowDowns = Arrays.asList(0,200,400,600,800,1000,1200,1400);
        
        List<Integer> lWindow = Arrays.asList(20);
        //List<Integer> futures = Arrays.asList(25);
        List<Integer> slowDowns = Arrays.asList(0);//200,400,600,800,1000);

        boolean proactivePlus = true;

        for (int l : lWindow)
            for (int f : futures)
                for (int ms : slowDowns) {
                System.out.println(l+ "_" +f);
                Properties prop = new Properties();

                // set key and value
                prop.setProperty("learningWindowSize", "" + l);
                prop.setProperty("edgePointsSize", "" + 3);
                prop.setProperty("futureInterval", "" + f);
                prop.setProperty("migrationSlowDown", "" +ms);
                prop.setProperty("proactivePlus", String.valueOf(proactivePlus));
                OutputStream output;
                if (proactivePlus)
                     output = new FileOutputStream(l + "_" + f + "_" +ms+"_plus.properties");
                else
                    output = new FileOutputStream(l + "_" + f + "_" +ms+".properties");
    //OutputStream output = new FileOutputStream(l + "_" + f+".properties");
                prop.store(output, null);
            }
    }


    @Test
    public void readConfig() throws IOException {
        InputStream input = new FileInputStream("config.properties");
        Properties prop = new Properties();

        // load a properties file
        prop.load(input);

        System.out.println("learningWindowSize: " + prop.getProperty("learningWindowSize"));
        System.out.println("edgePointsSize: " + prop.get("edgePointsSize"));
        System.out.println("safeguard: " + prop.get("safeguard"));
        System.out.println("futureInterval: " + prop.get("futureInterval"));
        System.out.println("migrationSlowDown: "+prop.get("migrationSlowDown"));
    }
}
