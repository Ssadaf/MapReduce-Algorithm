package Events;

import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FilePartMessage implements KompicsEvent {
    public String src;
    public String dst;
    public String owner;
    public List<String> part;

    public FilePartMessage(String src, String dst, String owner, List<String> part) {
        this.owner = owner;
        this.src = src;
        this.dst = dst;
        this.part = part;
    }
}