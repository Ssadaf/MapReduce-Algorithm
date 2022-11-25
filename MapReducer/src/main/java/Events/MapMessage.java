package Events;

import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class MapMessage implements KompicsEvent {
    public String src;
    public String dst;
    public HashMap<String, Integer> countingMap;

    public MapMessage(String src, String dst, HashMap<String, Integer> countingMap) {
        this.src = src;
        this.dst = dst;
        this.countingMap = countingMap;
    }
}