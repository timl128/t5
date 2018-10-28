package log.test;

import java.util.Comparator;

public class TimestampComparator implements Comparator<LogInfo> {
    @Override
    public int compare(LogInfo o1, LogInfo o2) {
        if (o1.getTimestamp() < o2.getTimestamp())
        {
            return -1;
        }
        if (o1.getTimestamp() > o2.getTimestamp())
        {
            return 1;
        }
        return 0;
    }
}
