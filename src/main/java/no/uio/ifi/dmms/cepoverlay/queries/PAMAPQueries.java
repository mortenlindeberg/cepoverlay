package no.uio.ifi.dmms.cepoverlay.queries;

import no.uio.ifi.dmms.cepoverlay.Main;

public class PAMAPQueries {
    public static String siddhiApp =
            "define stream ActivityStream(timestamp long, activityId int, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); " +
            "from ActivityStream[activityId == 20 AND heartRate > 180] select * " +
            "insert into OutputStream; ";

    public static String activityApp =
            "define stream actSource(timestamp long, activityId int); \n"+
            "from actSource\n"+
            "select * \n" +
            "insert into OutputStream; ";

    public static String heartApp =
            "define stream tempSource(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n"+
            "from tempSource \n"+
            "select * \n"+
            "insert into OutputStream; ";

    public static String activityJoinApp =
            "define stream actSource(timestamp long, count int, activityId int); \n" +
            "define stream tempSource(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +

                    "from actSource#window.length(1) as a join \n"+
                    "tempSource#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, h.heartRate as heartRate, h.temperatureHand as temp \n"+
                    "insert into joinedStream; \n" +
                    "from joinedStream#window.timeBatch(500 milliseconds) \n" +
                    "select max(atimestamp) as atimestamp, max(htimestamp) as htimestamp, avg(heartRate) as heartRate, avg(temp) as temp, count() as count \n"+
                    "insert into OutputStream";

    public static String activityJoinAppExt =
            "define stream actSource(timestamp long, count int, activityId int); \n" +
                    "define stream tempSource(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +

                    "from actSource#window.length(1) as a join \n"+
                    "tempSource#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, h.heartRate as heartRate, h.temperatureHand as temp \n"+
                    "insert into joinedStream; \n" +
                    "from joinedStream#window.externalTimeBatch(atimestamp, 1 sec) \n" +
                    "select max(atimestamp) as atimestamp, max(htimestamp) as htimestamp, avg(heartRate) as heartRate, avg(temp) as temp, count() as count \n"+
                    "insert into OutputStream";

    public static String activityPostJoinApp =
            "define stream postJoinSource(atimestamp long, htimestamp long, heartRate double, temperatureHand double, activity int); "+
            "from postJoinSource select * insert into OutputStream; ";

    public static String leftJoinPAMAPQueryApp =
            "define stream actSourceP1(timestamp long, activityId int); \n" +
                    "define stream tempSourceP1(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "define stream actSourceP2(timestamp long, activityId int); \n" +
                    "define stream tempSourceP2(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "" +
                    "from actSourceP1#window.length(1) as a join \n"+
                    "tempSourceP1#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP1; \n"+
                    ""+
                    "from actSourceP2#window.length(1) as a join \n"+
                    "tempSourceP2#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP2; \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select p1.atimestamp as p1time, p2.atimestamp as p2time, maximum(p1.temp, p2.temp) as mintemp\n"+
                    "insert into OutputStream; \n";

    public static String rightJoinPAMAPQueryApp =
            "define stream actSourceP3(timestamp long, activityId int); \n" +
                    "define stream tempSourceP3(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "define stream actSourceP4(timestamp long, activityId int); \n" +
                    "define stream tempSourceP4(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    ""+
                    "from actSourceP3#window.length(1) as a join \n"+
                    "tempSourceP3#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP3; \n"+
                    ""+
                    "from actSourceP4#window.length(1) as a join \n"+
                    "tempSourceP4#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP4; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select p3.atimestamp as p3time, p4.atimestamp as p4time, minimum(p3.temp, p4.temp) as mintemp\n"+
                    "insert into OutputStream; \n";

    public static String largePAMAPJoin =
            "define stream actSourceP1(timestamp long, activityId int); \n" +
                    "define stream tempSourceP1(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "define stream actSourceP2(timestamp long, activityId int); \n" +
                    "define stream tempSourceP2(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "define stream actSourceP3(timestamp long, activityId int); \n" +
                    "define stream tempSourceP3(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "define stream actSourceP4(timestamp long, activityId int); \n" +
                    "define stream tempSourceP4(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "" +
                    "from actSourceP1#window.length(1) as a join \n"+
                    "tempSourceP1#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP1; \n"+
                    ""+
                    "from actSourceP2#window.length(1) as a join \n"+
                    "tempSourceP2#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP2; \n"+
                    ""+
                    "from actSourceP3#window.length(1) as a join \n"+
                    "tempSourceP3#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP3; \n"+
                    ""+
                    "from actSourceP4#window.length(1) as a join \n"+
                    "tempSourceP4#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStreamP4; \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select p1.atimestamp as p1time, p2.atimestamp as p2time, minimum(p1.temp, p2.temp) as mintemp\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select p3.atimestamp as p3time, p4.atimestamp as p4time, minimum(p3.temp, p4.temp) as mintemp\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select s1.p1time as p1time, s1.p2time as p2time, s2.p3time as p3time, s2.p4time as p4time, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream;";

    public static String largePAMAPJoinPostPerson =
            "define stream OutputStreamP1(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP2(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP3(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP4(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 full outer join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select maximum(p1.atimestamp, p2.atimestamp) as maxtime, minimum(p1.temperature, p2.temperature) as mintemp\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 full outer join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select maximum(p3.atimestamp, p4.atimestamp) as maxtime, minimum(p3.temperature, p4.temperature) as mintemp\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 full outer join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream;";


    public static String finalJoinPAMAPQueryApp =
            "define stream OutputStream1(maxtime long, mintemp double); \n"+
                    "define stream OutputStream2(maxtime long, mintemp double); \n"+
                    "from OutputStream1#window.length(1) as s1 join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream; \n";






    public static String largePAMAPHRFilter =
            "define stream actSource(timestamp long, activityId int); \n" +
            "define stream tempSource(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
            "from actSource#window.length(1) as a join \n"+
            "tempSource#window.length(1) as h \n"+
            "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having heartRate < "+Main.PAMAP_LIMIT_HR+" \n"+
            "insert into OutputStream; ";

    public static String largePAMAPTempFilter =
            "define stream actSource(timestamp long, activityId int); \n" +
                    "define stream tempSource(timestamp long, heartRate double, temperatureHand double, temperatureChest double, temperatureAnkle double); \n" +
                    "from actSource#window.length(1) as a join \n"+
                    "tempSource#window.length(1) as h \n"+
                    "select a.timestamp as atimestamp, h.timestamp as htimestamp, a.activityId as activityId, h.heartRate as heartRate, h.temperatureHand as temp having temp < "+Main.PAMAP_LIMIT_TEMP+" \n"+
                    "insert into OutputStream; ";

    public static String extraLargePAMAPHRJoinPostPerson =
            "define stream OutputStreamP1(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP2(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP3(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP4(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP5(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP6(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP7(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP8(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 full outer join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select maximum(p1.atimestamp, p2.atimestamp) as maxtime, minimum(p1.heartRate, p2.heartRate) as minhr\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 full outer join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select maximum(p3.atimestamp, p4.atimestamp) as maxtime, minimum(p3.heartRate, p4.heartRate) as minhr\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStreamP5#window.length(1) as p5 full outer join \n"+
                    "OutputStreamP6#window.length(1) as p6 \n"+
                    "select maximum(p5.atimestamp, p6.atimestamp) as maxtime, minimum(p5.heartRate, p6.heartRate) as minhr\n"+
                    "insert into OutputStream3; \n"+
                    ""+
                    "from OutputStreamP7#window.length(1) as p7 full outer join \n"+
                    "OutputStreamP8#window.length(1) as p8 \n"+
                    "select maximum(p7.atimestamp, p8.atimestamp) as maxtime, minimum(p7.heartRate, p8.heartRate) as minhr\n"+
                    "insert into OutputStream4; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 full outer join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.minhr, s2.minhr) as minhr \n"+
                    "insert into OutputStream5;\n"+
                    ""+
                    "from OutputStream3#window.length(1) as s3 full outer join \n"+
                    "OutputStream4#window.length(1) as s4 \n"+
                    "select maximum(s3.maxtime, s4.maxtime) as maxtime, minimum(s3.minhr, s4.minhr) as minhr \n"+
                    "insert into OutputStream6;\n"+
                    ""+
                    "from OutputStream5#window.length(1) as s1 full outer join \n"+
                    "OutputStream6#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.minhr, s2.minhr) as minhr \n"+
                    "insert into OutputStream7";

    public static String extraLargePAMAPTempJoinPostPerson =
            "define stream OutputStreamP1(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP2(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP3(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP4(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP5(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP6(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP7(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP8(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 full outer join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select maximum(p1.atimestamp, p2.atimestamp) as maxtime, minimum(p1.temperature, p2.temperature) as mintemp\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 full outer join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select maximum(p3.atimestamp, p4.atimestamp) as maxtime, minimum(p3.temperature, p4.temperature) as mintemp\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStreamP5#window.length(1) as p5 full outer join \n"+
                    "OutputStreamP6#window.length(1) as p6 \n"+
                    "select maximum(p5.atimestamp, p6.atimestamp) as maxtime, minimum(p5.temperature, p6.temperature) as mintemp\n"+
                    "insert into OutputStream3; \n"+
                    ""+
                    "from OutputStreamP7#window.length(1) as p7 full outer join \n"+
                    "OutputStreamP8#window.length(1) as p8 \n"+
                    "select maximum(p7.atimestamp, p8.atimestamp) as maxtime, minimum(p7.temperature, p8.temperature) as mintemp\n"+
                    "insert into OutputStream4; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 full outer join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream5;\n"+
                    ""+
                    "from OutputStream3#window.length(1) as s3 full outer join \n"+
                    "OutputStream4#window.length(1) as s4 \n"+
                    "select maximum(s3.maxtime, s4.maxtime) as maxtime, minimum(s3.mintemp, s4.mintemp) as mintemp \n"+
                    "insert into OutputStream6;\n"+
                    ""+
                    "from OutputStream5#window.length(1) as s1 full outer join \n"+
                    "OutputStream6#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream; \n";


    public static String extraLargePAMAPHRWindowJoinPostPerson =
            "define stream OutputStreamP1(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP2(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP3(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP4(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP5(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP6(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP7(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP8(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 full outer join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select maximum(p1.atimestamp, p2.atimestamp) as maxtime, minimum(p1.heartRate, p2.heartRate) as minhr\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 full outer join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select maximum(p3.atimestamp, p4.atimestamp) as maxtime, minimum(p3.heartRate, p4.heartRate) as minhr\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStreamP5#window.length(1) as p5 full outer join \n"+
                    "OutputStreamP6#window.length(1) as p6 \n"+
                    "select maximum(p5.atimestamp, p6.atimestamp) as maxtime, minimum(p5.heartRate, p6.heartRate) as minhr\n"+
                    "insert into OutputStream3; \n"+
                    ""+
                    "from OutputStreamP7#window.length(1) as p7 full outer join \n"+
                    "OutputStreamP8#window.length(1) as p8 \n"+
                    "select maximum(p7.atimestamp, p8.atimestamp) as maxtime, minimum(p7.heartRate, p8.heartRate) as minhr\n"+
                    "insert into OutputStream4; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 full outer join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.minhr, s2.minhr) as minhr \n"+
                    "insert into OutputStream5;\n"+
                    ""+
                    "from OutputStream3#window.length(1) as s3 full outer join \n"+
                    "OutputStream4#window.length(1) as s4 \n"+
                    "select maximum(s3.maxtime, s4.maxtime) as maxtime, minimum(s3.minhr, s4.minhr) as minhr \n"+
                    "insert into OutputStream6;\n"+
                    ""+
                    "from OutputStream5#window.length(1) as s1 full outer join \n"+
                    "OutputStream6#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.minhr, s2.minhr) as minhr \n"+
                    "insert into OutputStream7; \n" +
                    "\n" +
                    "from OutputStream7#window.externalTimeBatch(maxtime, 1 sec) \n" +
                    "select max(maxtime) as time, avg(minhr) as hr \n" +
                    "insert into OutputStream;";

    public static String extraLargePAMAPTempWindowJoinPostPerson =
            "define stream OutputStreamP1(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP2(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP3(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP4(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP5(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP6(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP7(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    "define stream OutputStreamP8(atimestamp long, htimestamp long, activityId int, heartRate double, temperature double); \n"+
                    ""+
                    "from OutputStreamP1#window.length(1) as p1 full outer join \n"+
                    "OutputStreamP2#window.length(1) as p2 \n"+
                    "select maximum(p1.atimestamp, p2.atimestamp) as maxtime, minimum(p1.temperature, p2.temperature) as mintemp\n"+
                    "insert into OutputStream1; \n"+
                    ""+
                    "from OutputStreamP3#window.length(1) as p3 full outer join \n"+
                    "OutputStreamP4#window.length(1) as p4 \n"+
                    "select maximum(p3.atimestamp, p4.atimestamp) as maxtime, minimum(p3.temperature, p4.temperature) as mintemp\n"+
                    "insert into OutputStream2; \n"+
                    ""+
                    "from OutputStreamP5#window.length(1) as p5 full outer join \n"+
                    "OutputStreamP6#window.length(1) as p6 \n"+
                    "select maximum(p5.atimestamp, p6.atimestamp) as maxtime, minimum(p5.temperature, p6.temperature) as mintemp\n"+
                    "insert into OutputStream3; \n"+
                    ""+
                    "from OutputStreamP7#window.length(1) as p7 full outer join \n"+
                    "OutputStreamP8#window.length(1) as p8 \n"+
                    "select maximum(p7.atimestamp, p8.atimestamp) as maxtime, minimum(p7.temperature, p8.temperature) as mintemp\n"+
                    "insert into OutputStream4; \n"+
                    ""+
                    "from OutputStream1#window.length(1) as s1 full outer join \n"+
                    "OutputStream2#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream5;\n"+
                    ""+
                    "from OutputStream3#window.length(1) as s3 full outer join \n"+
                    "OutputStream4#window.length(1) as s4 \n"+
                    "select maximum(s3.maxtime, s4.maxtime) as maxtime, minimum(s3.mintemp, s4.mintemp) as mintemp \n"+
                    "insert into OutputStream6;\n"+
                    ""+
                    "from OutputStream5#window.length(1) as s1 full outer join \n"+
                    "OutputStream6#window.length(1) as s2 \n"+
                    "select maximum(s1.maxtime, s2.maxtime) as maxtime, minimum(s1.mintemp, s2.mintemp) as mintemp \n"+
                    "insert into OutputStream7; \n" +
                    "\n" +
                    "from OutputStream7#window.externalTimeBatch(maxtime, 1 sec)  \n" +
                    "select max(maxtime) as time, avg(mintemp) as temp \n" +
                    "insert into OutputStream;";



    public static String postJoinPAMAPHRQuery =
            "define Stream InputStream (maxtime long, minhr double); \n"+
            "from InputStream select * insert into OutputStream";

    public static String postJoinPAMAPTempQuery =
            "define Stream InputStream (maxtime long, mintemp double); \n"+
                    "from InputStream select * insert into OutputStream";

}
