package Events;

import java.util.ArrayList;
import java.util.Set;

import se.sics.kompics.KompicsEvent;


public class AnnounceRootMessage implements KompicsEvent {
    public String initiator;
    public String src;
    public String dst;
    public Set<String> leafNodes;

    public AnnounceRootMessage(String initiator, String src, String dst, Set<String> leafNodes) {
        this.initiator = initiator;
        this.src = src;
        this.dst = dst;
        this.leafNodes = leafNodes;
    }
}