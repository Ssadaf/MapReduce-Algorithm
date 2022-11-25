package Components;
import Events.*;
import Ports.EdgePort;
import se.sics.kompics.*;

import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ConstantPackage.Constants.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;


public class Node extends ComponentDefinition {
    Positive<EdgePort> recievePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    public String nodeName;
    HashMap<String,Integer> neighbours = new HashMap<>();

    Boolean isStarterNode ;
    public String graphSizeParentName;

    Boolean isRoot;
    Boolean nodeType;
    public String parentName = BOTTOM;

    int receivedEcho;
    int echoChilldNum;
    int graphSize;
    HashMap<String, Double> avgDistances = new HashMap<>();
    HashMap<String, Integer> distancesFromMe = new HashMap<>();

    Set<String> leafs = new HashSet<>();
    int receivedAnnounceRoot;
    int receivedMaps;

    HashMap<String, Integer> wordCountMap = new HashMap<>();


    Handler echoHandler = new Handler<EchoMessage>() {
        @Override
        public void handle(EchoMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                receivedEcho ++;
                echoChilldNum += event.childNum;


                if ((graphSizeParentName.equals(BOTTOM)) & (!nodeName.equals(event.initiator))) { //first time
                    graphSizeParentName = event.src;

                    if(neighbours.size() > 1){
                        for (Map.Entry<String, Integer> neighbor : neighbours.entrySet()) {
                            if(! graphSizeParentName.equals(neighbor.getKey())) {
                                System.out.println(String.format( "%s sent echo down to %s", nodeName, neighbor.getKey() ));
                                trigger(new EchoMessage(event.initiator, nodeName, neighbor.getKey(), 0), sendPort);
                            }
                        }
                    }
                    else{
                        echoChilldNum += 1;
                            System.out.println(String.format( "%s sent echo up to %s with value %d(case1)", nodeName, graphSizeParentName, echoChilldNum ));
                            trigger(new EchoMessage(event.initiator, nodeName, graphSizeParentName, echoChilldNum), sendPort);
                        }
                    }
                    else if(receivedEcho == neighbours.size()){
                        if(!nodeName.equals(event.initiator)){
                            echoChilldNum += 1;
                            System.out.println(String.format( "%s sent echo up to %s with value %d(case2)", nodeName, graphSizeParentName, echoChilldNum ));
                            trigger(new EchoMessage(event.initiator, nodeName, graphSizeParentName, echoChilldNum), sendPort);
                        }
                        else{//echo completed
                            graphSize = echoChilldNum + 1;
                            System.out.println(String.format( "Echo completed : graph size is %d and %s started its distance broadcast",graphSize, nodeName ));
                            System.out.println("_______________________________________________________________");
                            HashMap<String, Integer> distancesMap = new HashMap<>();
                            distancesMap.put(nodeName, 0);
                            for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                            {
                                trigger(new BroadcastSizeMessage(nodeName, neighbor.getKey(), graphSize), sendPort);
                                trigger(new DistancesMessage(nodeName, nodeName, neighbor.getKey(), distancesMap), sendPort);
                            }
                        }
                    }
                }
            }
        };

    Handler broadcastSizeHandler = new Handler<BroadcastSizeMessage>() {
        @Override
        public void handle(BroadcastSizeMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                graphSize = event.graphSize;

                HashMap<String, Integer> distancesMap = new HashMap<>();
                distancesMap.put(nodeName, 0);

                System.out.println(String.format( "%s received graph size = %d and started its distance broadcast", nodeName, graphSize ));
                for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                {
                    trigger(new DistancesMessage(nodeName, nodeName, neighbor.getKey(), distancesMap), sendPort);

                    if(! graphSizeParentName.equals(neighbor.getKey())) {
                        trigger(new BroadcastSizeMessage(nodeName, neighbor.getKey(), graphSize), sendPort);
                    }
                }
            }
        }
    };

    Handler distanceHandler = new Handler<DistancesMessage>() {
        @Override
        public void handle(DistancesMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                HashMap<String, Integer> thisDistanceMap = event.distances;
                Integer myDistance = thisDistanceMap.get(event.src) + neighbours.get(event.src);

                distancesFromMe.put(event.initiator, myDistance);
                thisDistanceMap.put(nodeName, myDistance);
    //            System.out.println(String.format( "%s received distances map of %s and its distance is %d",
    //                    nodeName, event.initiator, myDistance));
                for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                {
                    if(! event.src.equals(neighbor.getKey())) {
                        trigger(new DistancesMessage(event.initiator,nodeName, neighbor.getKey(), thisDistanceMap), sendPort);
                    }
                }
                if(distancesFromMe.size() == graphSize){
                    double averageDist = 0;
                    for( Map.Entry<String, Integer> node : distancesFromMe.entrySet())
                    {
                        averageDist += node.getValue();
                    }
                    averageDist = averageDist/(graphSize-1);

                    avgDistances.put(nodeName, averageDist);
                    findRoot();

                    for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                    {
                        trigger(new AvgDistMessage(nodeName, nodeName, neighbor.getKey(), averageDist ), sendPort);
                    }
                    System.out.println(String.format( "%s node has average distance of %f", nodeName, averageDist));
                }
            }
        }
    };

    Handler avgDistanceHandler = new Handler<AvgDistMessage>() {
        @Override
        public void handle(AvgDistMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                avgDistances.put(event.initiator, event.averageDistance);
                for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                {
                    if(! event.src.equals(neighbor.getKey())) {
                        trigger(new AvgDistMessage(event.initiator, nodeName, neighbor.getKey(), event.averageDistance), sendPort);
                    }
                }

                findRoot();

            }
        }
    };

    private void findRoot(){
        if(avgDistances.size() == graphSize){
            Double minAvg = INFINITY;
            String rootToBe = BOTTOM;
            for( Map.Entry<String, Double> entry : avgDistances.entrySet())
            {
                if( minAvg > entry.getValue()){
                    rootToBe = entry.getKey();
                    minAvg = entry.getValue();
                }
                else if ((minAvg == entry.getValue()) & (rootToBe.compareTo(entry.getKey())>0)){
                    rootToBe = entry.getKey();
                    minAvg = entry.getValue();
                }
            }
            isRoot = rootToBe.equals(nodeName);
            if(isRoot) {
                isRoot = true;
                nodeType = REDUCER;
                leafs = new HashSet<>();
                System.out.println(String.format( "Root is %s and started announcing root", nodeName));

                for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                {
                    trigger(new AnnounceRootMessage(nodeName, nodeName, neighbor.getKey(), leafs), sendPort);
                }
            }
        }
    }

    Handler announceRootHandler = new Handler<AnnounceRootMessage>() {
        @Override
        public void handle(AnnounceRootMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                receivedAnnounceRoot ++ ;
                leafs.addAll(event.leafNodes);

                if((parentName.equals(BOTTOM)) & (!nodeName.equals(event.initiator))){//first time
                    parentName = event.src;

                    if(neighbours.size() > 1){
                        nodeType = REDUCER;
                        for (Map.Entry<String, Integer> neighbor : neighbours.entrySet()) {
                            if(! parentName.equals(neighbor.getKey())) {
                                trigger(new AnnounceRootMessage(event.initiator, nodeName, neighbor.getKey(), event.leafNodes), sendPort);
                            }
                        }
                    }
                    else{ // it's a leaf node
                        nodeType = MAPPER;
                        leafs = event.leafNodes;
                        leafs.add(nodeName);
                        trigger(new AnnounceRootMessage(event.initiator, nodeName, parentName,leafs), sendPort);
                    }
                }
                else if(receivedAnnounceRoot == neighbours.size()){
                    if(!nodeName.equals(event.initiator)){
                        trigger(new AnnounceRootMessage(event.initiator, nodeName, parentName,leafs), sendPort);
                    }
                    else{//echo completed
                        System.out.println(String.format( "Root Announce Echo completed : leaf number is %d", leafs.size() ));
                        System.out.println("_______________________________________________________________");
                        sendTextToMappers();
                    }
                }
            }
        }
    };

    private void sendTextToMappers(){
        try{
            List<String> lines = Files.readAllLines(Paths.get(INPUT_FILE_PATH), StandardCharsets.UTF_8);
            System.out.println(String.format("number of file lines %d", lines.size()));
            int partSize = lines.size()/leafs.size() + 1;
            int i = 0;
            for(String leaf : leafs){
                if(i < leafs.size()-1){
                    List<String> partLines = lines.subList(i * partSize, (i + 1) * partSize);
                    broadcastFilePart(leaf, partLines);
                }
                else{
                    List<String> partLines = lines.subList(i * partSize, lines.size());
                    broadcastFilePart(leaf, partLines);
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastFilePart(String leafName, List<String> part){
        for( Map.Entry<String, Integer> neighbor : neighbours.entrySet()){
            if(! parentName.equals(neighbor.getKey())){
                trigger(new FilePartMessage(nodeName, neighbor.getKey(), leafName, part), sendPort);
            }
        }
    }

    Handler filePartHandler = new Handler<FilePartMessage>() {
        @Override
        public void handle(FilePartMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                if(nodeName.equals(event.owner)){
                    mapWordsToCounts(event.part);
                }
                else{
                    for( Map.Entry<String, Integer> neighbor : neighbours.entrySet()){
                        if(! event.src.equals(neighbor.getKey())){
                            trigger(new FilePartMessage(nodeName, neighbor.getKey(), event.owner, event.part), sendPort);
                        }
                    }
                }
            }
        }
    };

    private void mapWordsToCounts(List<String> data){
        System.out.println(String.format("In Mapper %s", nodeName));
//        for(int i = 0 ; i < data.size() ; i++){
//            System.out.println(data.get(i));
//        }

        for(int i = 0 ; i < data.size() ; i++){
            String line  = data.get(i).replaceAll("[^a-zA-Z0-9 ]", " ").toLowerCase();
            String[] words = line.split(" ");
            for(String word : words){
                if (! word.equals("")){
                    if(!wordCountMap.containsKey(word)){
                        wordCountMap.put(word, 1);
                    }else{
                        wordCountMap.put(word, wordCountMap.get(word) + 1);
                    }
                }
            }
        }
//        for(Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
//            String outputLine = entry.getKey() + ":" +  entry.getValue() + "\n";
//            System.out.println(outputLine);
//        }


        trigger(new MapMessage(nodeName, parentName, wordCountMap), sendPort);
    }

    Handler mapHandler = new Handler<MapMessage>() {
        @Override
        public void handle(MapMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                receivedMaps += 1;
                for(Map.Entry<String, Integer> entry : event.countingMap.entrySet()) {
                    if(! wordCountMap.containsKey(entry.getKey())){
                        wordCountMap.put(entry.getKey(), entry.getValue());
                    }else{
                        wordCountMap.put(entry.getKey(), wordCountMap.get(entry.getKey()) + entry.getValue());
                    }
                }
                if(!isRoot & receivedMaps == neighbours.size()-1){
                    trigger(new MapMessage(nodeName, parentName, wordCountMap), sendPort);
                }
                if(isRoot & receivedMaps == neighbours.size()){
                    System.out.println("Procedure completed writing to file");
                    writeMapToFile();
                }
            }
        }
    };

    private void writeMapToFile(){
        Path path = Paths.get("src/main/java/output.txt");
        OpenOption[] options = new OpenOption[] {CREATE, APPEND};
        try {
            for(Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
                String outputLine = entry.getKey() + ":" +  entry.getValue() + "\n";
                Files.write(path, outputLine.getBytes(), options);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            distancesFromMe.put(nodeName, 0);
            graphSizeParentName = BOTTOM;
            receivedEcho = 0;
            echoChilldNum = 0;
            receivedAnnounceRoot = 0;
            parentName = BOTTOM;
            receivedMaps = 0;

            if(isStarterNode){
                System.out.println(String.format( "Echo started by %s", nodeName ));
                for( Map.Entry<String, Integer> neighbor : neighbours.entrySet())
                {
                    trigger(new EchoMessage(nodeName, nodeName, neighbor.getKey(), 0), sendPort);
                }
            }

        }
    };


    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        System.out.println("initNode :" + initMessage.nodeName);
        this.neighbours = initMessage.neighbours;
        this.isStarterNode = initMessage.isStarterNode;
        subscribe(startHandler, control);
        subscribe(echoHandler, recievePort);
        subscribe(broadcastSizeHandler, recievePort);
        subscribe(distanceHandler, recievePort);
        subscribe(avgDistanceHandler, recievePort);
        subscribe(announceRootHandler, recievePort);
        subscribe(mapHandler, recievePort);
        subscribe(filePartHandler, recievePort);

    }


}

