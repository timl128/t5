package log.test;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.text.ParseException;
import java.util.*;

import static log.test.SystemHealth.TIME_FIELD_JSON;
import static log.test.SystemHealth.TRACE_ID_FIELD_JSON;
import static org.junit.Assert.assertEquals;


public class SystemHealthTest {

    @Test
    public void extractLogWithSpecificTraceId() throws ParseException {

        SystemHealth systemHealth = new SystemHealth();

        String traceId = "a";
        HashMap<String, List<LogInfo>> logMap = new HashMap<>();
        Set<String> errorSet = new TreeSet<>();
        errorSet.add(traceId);

        JSONArray jsonArray = new JSONArray();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TRACE_ID_FIELD_JSON,traceId);
        jsonObject.put(TIME_FIELD_JSON,"2018-10-26T02:12:04+11:00");

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put(TRACE_ID_FIELD_JSON,"b");
        jsonObject1.put(TIME_FIELD_JSON,"2018-10-21T02:12:04+11:00");

        jsonArray.add(jsonObject);
        jsonArray.add(jsonObject1);


        systemHealth.extractLogWithSpecificTraceId(logMap,errorSet,jsonArray);

        assertEquals(logMap.size(),1);
    }

    @Test
    public void testPriorityQueue(){


        Queue<LogInfo> logInfoQueue = new PriorityQueue<>(new TimestampComparator());

        LogInfo logInfo1 = new LogInfo(100, new JSONObject());
        LogInfo logInfo2 = new LogInfo(50, new JSONObject());

        logInfoQueue.add(logInfo1);
        logInfoQueue.add(logInfo2);

        assertEquals(logInfoQueue.peek(),logInfo2);

    }


}