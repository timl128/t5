package log.test;

import org.json.simple.JSONObject;

public class LogInfo {

    private long timestamp;

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    private JSONObject jsonObject;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public LogInfo(long timestamp, JSONObject jsonObject){
        this.timestamp = timestamp;
        this.jsonObject = jsonObject;
    }
}
