package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(EchoMessage.class);
    positive(BroadcastSizeMessage.class);
    positive(DistancesMessage.class);
    positive(AvgDistMessage.class);
    positive(AnnounceRootMessage.class);
    positive(FilePartMessage.class);
    positive(MapMessage.class);


    negative(EchoMessage.class);
    negative(BroadcastSizeMessage.class);
    negative(DistancesMessage.class);
    negative(AvgDistMessage.class);
    negative(AnnounceRootMessage.class);
    negative(FilePartMessage.class);
    negative(MapMessage.class);

}}
