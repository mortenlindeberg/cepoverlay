package no.uio.ifi.dmms.cepoverlay.source;

import java.io.*;

import static no.uio.ifi.dmms.cepoverlay.source.SimpleSourceThread.openFile;

public class SourceUtility {

    private static double nextHR = Double.NaN;
    private static double lastHR = Double.NaN;
    private static double slope = 0;
    private static int steps = 0;
    private static int remainingSteps = 0;


    public static void shortenSource(String inFile, String outFile, int offset) throws IOException {
        BufferedReader reader = openFile(inFile);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
        String line = null;
        ActivityTuple t = null;

        while ((line = reader.readLine()) != null) { // Discard the tuples that are "past" in time following system warmup
            String lineArr[] = line.split(" ");
            if (line.length() < 10) break;

            double orgTimestamp = Double.parseDouble(lineArr[0]);
            if (orgTimestamp > offset) {
                lineArr[0] = ""+(orgTimestamp-offset);

                for (int i = 0; i < lineArr.length; i++)
                    out.print(lineArr[i]+" ");

                out.print("\n");
            }
        }
    }

    public static void fixNaN(String inFile, String outFile) throws IOException, InterruptedException {

        BufferedReader reader = openFile(inFile);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
        String line = null;
        ActivityTuple t = null;

        while ((line = reader.readLine()) != null) { // Discard the tuples that are "past" in time following system warmup
            Thread.sleep(1);
            String lineArr[] = line.split(" ");
            if (line.length() < 10) break;
            double hr = Double.parseDouble(lineArr[2]);

            if (Double.isNaN(hr)) {
                reader.mark(200000);
                hr = getHartRate(reader);
                reader.reset();
            }
            lineArr[2] = ""+hr;
            for (int i = 0; i < lineArr.length; i++)
                out.print(lineArr[i]+" ");

            out.print("\n");
        }
    }

    private static double getHartRate(BufferedReader reader) throws IOException {
        /* We are either on the slope, or we need to find the next */
        if (remainingSteps > 0) {
            double hr = nextHR + steps*slope;
            steps++;
            remainingSteps--;
            return hr;
        }

        String line = "Not null";
        double hr = 0;
        int i = 0;

        while ( (line = reader.readLine()) != null) { // Discard the tuples that are "past" in time following system warmup
            //System.out.println("ahead> "+line);
            if (line == null || line.length() < 10) break;
            String lineArr[] = line.split(" ");
            hr = Double.parseDouble(lineArr[2]);
            if (!Double.isNaN(hr)) {

                /* If this is a first, then just return whatever that is */
                if (Double.isNaN(lastHR)) {
                    lastHR = hr;
                    return hr;
                }

                steps = 0;
                remainingSteps = i;
                slope = (lastHR - hr) / (1+i);
                lastHR = hr;
                //System.out.println("Found new HR: "+hr+" . Slope: "+slope+" remaining steps: "+remainingSteps+" "+lastHR+"-"+hr+" / "+i+"+1");
                return hr;
            }
        }
        System.out.println("Did not find new HR: "+hr+" . Slope: "+slope);
        return hr;
    }

}
