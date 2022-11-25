package Events;

import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class EchoMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String initiator;
    public Integer childNum;

    public EchoMessage(String initiator, String src, String dst, Integer childNum) {
        this.src = src;
        this.dst = dst;
        this.initiator = initiator;
        this.childNum = childNum;
    }
}

