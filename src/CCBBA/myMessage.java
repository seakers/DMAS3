package CCBBA;

import madkit.kernel.Message;

import java.util.Vector;

public class myMessage extends Message {
    public IterationResults myResults;
    public boolean consensus;

    public myMessage(IterationResults newResults, boolean newConsensus){
        myResults = newResults;
        consensus = newConsensus;
    }
}
