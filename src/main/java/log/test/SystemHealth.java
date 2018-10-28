package log.test;

import org.json.simple.JSONArray;

import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class SystemHealth {

    public final static String TIME_FIELD_JSON = "time";
    public final static String TRACE_ID_FIELD_JSON = "trace_id";
    public final static String PARENT_ID_JSON = "parent_span_id";
    public final static String APP_FIELD_JSON = "app";
    public final static String SPAN_ID_FIELD_JSON = "span_id";
    public final static String COMPONENT_FIELD_JSON = "component";
    public final static String MESSAGE_FIELD_JSON = "msg";
    public final static String ERROR_FIELD_JSON = "error";
    public final static int NUM_OF_THREAD = 20;

    public SystemHealth(){

    }

    public static void main(String[] args)  {


        boolean runMultiThread = false;
        String filename = args[0];
        String concurrency = args[1];

        if(concurrency.equals("true"))
            runMultiThread = true;

        SystemHealth systemHealth = new SystemHealth();
        try {
            systemHealth.process(filename,runMultiThread);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * process
     * @param filename
     * @throws IOException
     * @throws ParseException
     * @throws java.text.ParseException
     */
    public void process(String filename, boolean concurrency) throws IOException, ParseException,
            java.text.ParseException {

        long startTime = System.currentTimeMillis();
        HashMap<String, List<LogInfo>> logMap = new HashMap<>();

        //Read file
        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new FileReader(filename));


        Set<String> errorSet = new TreeSet<>();
        findTraceIdWithError(errorSet,a);


        System.out.println("Number of failed transactions :  " + errorSet.size() );


        //HashMap with linked list
        extractLogWithSpecificTraceId(logMap,errorSet,a);


        //multi thread sort each linked list by time
        // Time complexity O(n)

        if(concurrency){
            multiThreadOnly(logMap,startTime);
        }else{
            oneThreadOnly(logMap,startTime);
        }


    }


    public void oneThreadOnly(HashMap<String, List<LogInfo>> logMap, long startTime){
        for (Map.Entry<String,List<LogInfo>> item:logMap.entrySet() )
        {
            sortLogMessage(item.getValue());
        }
        printTime(startTime);
    }


    public void printTime(long startTime){
        System.out.println( "Time : " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public void multiThreadOnly(HashMap<String, List<LogInfo>> logMap, long startTime){
        ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREAD);
        List<Callable<Integer>> concurrency = new ArrayList<>();
        for (Map.Entry<String,List<LogInfo>> item:logMap.entrySet() )
        {
            concurrency.add(processCallable(item.getValue()));
        }
        try {
            executor.invokeAll(concurrency);
            printTime(startTime);
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Callable<Integer> processCallable(List<LogInfo> logInfoList){
        sortLogMessage(logInfoList);
        return () -> 10;
    }

    /**
     * 1st scanning
     * find the trace id of the json contains `error` and it is true
     * @param errorSet
     * @param jsonArray
     */
    public void findTraceIdWithError(Set<String> errorSet,JSONArray jsonArray ){

        // Time complexity O(n)
        for (Object b :jsonArray) {
            JSONObject log = (JSONObject) b;
            Boolean isError = (Boolean) log.get(ERROR_FIELD_JSON);
            String traceId = (String) log.get(TRACE_ID_FIELD_JSON);

            if(isError != null && isError)
                errorSet.add(traceId);
        }
    }

    /**
     * 2nd scanning
     * @param logMap
     * @param errorSet
     * @param jsonArray
     */
    public void extractLogWithSpecificTraceId(HashMap<String, List<LogInfo>> logMap,
                                                Set<String> errorSet,
                                              JSONArray jsonArray) throws java.text.ParseException {
        //O(n)
        for (Object b :jsonArray) {
            JSONObject log = (JSONObject) b;
            String traceId = (String) log.get(TRACE_ID_FIELD_JSON);

            // find the test

            //map.contains : O(1)
            if(errorSet.contains(traceId)){
                String date = (String) log.get(TIME_FIELD_JSON);
                date = date.replace("T"," ");
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");
                Date dateObject = sf.parse(date);
                long timestamp = dateObject.getTime();
                LogInfo logInfo = new LogInfo(timestamp,(JSONObject)b);
                if(logMap.containsKey(traceId)){
                    logMap.get(traceId).add(logInfo);
                }
                else{
                    List<LogInfo> logInfoList = new LinkedList<>();
                    logInfoList.add(logInfo);
                    logMap.put(traceId,logInfoList);
                }
            }


        }
    }

    /**
     * sort message
     * @param logInfos
     */
    public void sortLogMessage(List<LogInfo> logInfos) {


        Map<String,Queue<LogInfo>> logMap = new HashMap<>();

        //O(n)
        for(LogInfo logInfo:logInfos){

            JSONObject jsonObject = logInfo.getJsonObject();
            String parentSpanId = (String) jsonObject.get(PARENT_ID_JSON);


            if(logMap.containsKey(parentSpanId)){
                logMap.get(parentSpanId).add(logInfo);
            }
            else{
                TimestampComparator timestampComparator = new TimestampComparator();
                Queue<LogInfo> tempQueue = new PriorityQueue<>(timestampComparator);

                tempQueue.add(logInfo);
                logMap.put(parentSpanId,tempQueue);

            }
        }

        printLogMessage(logMap);


    }

    /**
     * print log message from root level
     * @param logMap
     */
    public void printLogMessage(Map<String,Queue<LogInfo>> logMap ){

        Queue<LogInfo> rootLevel = logMap.get(null);
        printLogMessageProcess(logMap,rootLevel,0);

    }


    /**
     * construct the failure causes
     * @param logMap
     * @param queue
     * @param level
     */
    public void printLogMessageProcess(Map<String,Queue<LogInfo>> logMap,
                                       Queue<LogInfo> queue,
                                       int level){

        while(!queue.isEmpty()){
            LogInfo logInfo = queue.remove();
            JSONObject jsonObject = logInfo.getJsonObject();

            printMessage(level,jsonObject);

            if(jsonObject.get(SPAN_ID_FIELD_JSON) != null &&
                    logMap.containsKey(jsonObject.get(SPAN_ID_FIELD_JSON)))
            {
                Queue<LogInfo> newLevelQueue = logMap.get(jsonObject.get(SPAN_ID_FIELD_JSON));
                printLogMessageProcess( logMap,newLevelQueue,level +1);
                logMap.remove(jsonObject.get(SPAN_ID_FIELD_JSON));
            }


        }

    }

    /**
     * root level is level 0
     * @param level
     * @param jsonObject
     * @return
     */
    public String generateLogMessage(int level, JSONObject jsonObject){

        String spacing = "";
        for(int i = 0 ; i < level; i ++){
            spacing += " ";
        }

        return String.format("%s- %s %s %s %s" , spacing,
                jsonObject.get(TIME_FIELD_JSON),
                jsonObject.get(APP_FIELD_JSON),
                jsonObject.get(COMPONENT_FIELD_JSON),
                jsonObject.get(MESSAGE_FIELD_JSON));
    }

    public void printMessage(int level, JSONObject jsonObject){
        String message = generateLogMessage(level,jsonObject);
        System.out.println(message);
    }
}
