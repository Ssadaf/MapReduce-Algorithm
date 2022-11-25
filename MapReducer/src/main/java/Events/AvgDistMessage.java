package Events;

import se.sics.kompics.KompicsEvent;


public class AvgDistMessage implements KompicsEvent {
    public String initiator;
    public String src;
    public String dst;
    public Double averageDistance;

    public AvgDistMessage(String initiator, String src, String dst, Double averageDistance) {
        this.initiator = initiator;
        this.src = src;
        this.dst = dst;
        this.averageDistance = averageDistance;
    }
}