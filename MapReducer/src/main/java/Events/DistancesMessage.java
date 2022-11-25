package Events;

import se.sics.kompics.KompicsEvent;

import java.util.HashMap;

public class DistancesMessage implements KompicsEvent {
    public String initiator;
    public String src;
    public String dst;
    public HashMap<String, Integer> distances;

    public DistancesMessage(String initiator, String src, String dst, HashMap<String, Integer> distances) {
        this.initiator = initiator;
        this.src = src;
        this.dst = dst;
        this.distances = distances;
    }
}