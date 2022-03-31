package no.uio.ifi.dmms.cepoverlay.queries;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class    ConfigCreator {

    public static void createConfig(String configString) throws IOException {

        HashMap<String, String> propMap = new HashMap<String, String>();

        propMap.put("proactivePlus","false");
        propMap.put("proactivePAMAPPlus","false");
        propMap.put("edgePointsSize","10");
        propMap.put("futureInterval","200");
        propMap.put("learningWindowSize","20");
        propMap.put("migrationSlowDown","0");
        propMap.put("adaptationThreshold","0");
        propMap.put("queryId","0");
        propMap.put("windowAware", "false");
        propMap.put("windowSize","1000");
        propMap.put("migrationTime","500");


        List<String> configProps = Arrays.asList(configString.split(","));

        for (String prop : configProps) {
            String arr[] = prop.split("=");
            String propKey = arr[0];
            String propValue = arr[1];
            propMap.put(propKey, propValue);
        }

        Properties prop = new Properties();
        for (String p : propMap.keySet()) {
            prop.setProperty(p,propMap.get(p));
        }
        OutputStream output = new FileOutputStream("config.properties");
        prop.store(output, null);
    }
}

