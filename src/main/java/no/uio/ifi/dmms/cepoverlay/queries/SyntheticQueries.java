package no.uio.ifi.dmms.cepoverlay.queries;

import no.uio.ifi.dmms.cepoverlay.Main;

public class SyntheticQueries {

    public static String joinApp = "define stream LeftStream(timestamp long, roomNo int, temp double); " +
            "define stream RightStream(timestamp long, roomNo int, isOn bool); " +
            "@info(name = 'queryengine')" +
            "from " +
            "LeftStream#reorder:kslack(timestamp, TRUE)[temp > "+ Main.LIMIT+"]#window.length(1) as T unidirectional join " +
            "RightStream#reorder:kslack(timestamp, TRUE)[isOn == false]#window.length(1) as R " +
            "on T.roomNo == R.roomNo " +
            "select T.timestamp as ttime, R.timestamp as rtime, T.temp as temp insert into OutputStream;";

    public static String postJoinApp = "define stream PostJoinStream(leftTimestamp long, rightTimestamp long, temperature int);" +
            "from PostJoinStream select * " +
            "insert into OutputStream;";
}
