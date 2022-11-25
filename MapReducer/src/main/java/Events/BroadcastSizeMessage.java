package Events;

import se.sics.kompics.KompicsEvent;

public class BroadcastSizeMessage implements KompicsEvent {
    public String src;
    public String dst;
    public Integer graphSize;

    public BroadcastSizeMessage(String src, String dst, Integer graphSize) {
        this.src = src;
        this.dst = dst;
        this.graphSize = graphSize;
    }
}