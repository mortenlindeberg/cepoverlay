package no.uio.ifi.dmms.cepoverlay.queries;

public class PowerTempQueries {
    public static String tempAggQueryApp =
            "define stream temperatureStream(timestamp long, city string, temperature double, humidity double); " +
                    "from temperatureStream#window.externalTimeBatch(timestamp, 1 hour) " +
                    "select max(timestamp) as timestamp, avg(temperature) as temperature, avg(humidity) as humidity, count() as count " +
                    "insert into OutputStream; ";

    public static String tempQueryApp =
            "define stream temperatureStream(timestamp long, temperature double, humidity double, count int); " +
                    "from temperatureStream select * " +
                    "insert into OutputStream; ";

    public static String loadQueryApp =
            "define stream loadStream(timestamp long, trend double, load double); " +
                    "from loadStream select * " +
                    "insert into OutputStream; ";

    public static String tempJoinQueryApp =
            "define stream temperatureStream(timestamp long, temperature double, humidity double, count int); " +
                    "define stream loadStream(timestamp long, trend double, load double); " +
                    "from loadStream#window.externalTimeBatch(timestamp, 1 hour) as l join " +
                    "temperatureStream#window.externalTimeBatch(timestamp, 1 hour) as t on l.timestamp == t.timestamp \n" +
                    "select max(l.timestamp) as timestamp, max(t.timestamp) as timestamp2, l.load as load, t.temperature as temperature, t.humidity as humidity, t.count as count "+
                    "insert into OutputStream; ";

    public static String tempPostJoinApp =
            "define stream PostJoinStream(tempTimestamp long, loadTimestamp long, load double, temperature double, humidity double, count int); " +
                    "from PostJoinStream select * insert into OutputStream; ";
}
