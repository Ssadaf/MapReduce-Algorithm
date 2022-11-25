package Events;

import Components.Node;
import se.sics.kompics.Init;

import java.util.HashMap;

public class InitMessage extends Init<Node> {
    public String nodeName;
    public boolean isStarterNode = false;
    public HashMap<String,Integer> neighbours = new HashMap<>();

    public InitMessage(String nodeName, boolean isStarterNode,
                       HashMap<String,Integer> neighbours) {
        this.nodeName = nodeName;
        this.isStarterNode = isStarterNode;
        this.neighbours = neighbours;
    }
}