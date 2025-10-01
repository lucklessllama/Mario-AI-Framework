package agents.MichaelAllen;

import agents.allen.PathFindToTile;
import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

import java.util.ArrayList;
import java.util.List;

public class Agent implements MarioAgent {
    NextMoveFinder NMF;
    List<boolean[]> actions;
    int[] beenToo;
    int bestDist;
    float t = 1;// represent how long sense we map progress
    scoreValueMode s = new scoreValueMode();
    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        float[] levelSize =model.getLevelFloatDimensions();
        beenToo = new int[(int)(levelSize[0]/12)];
        actions = new ArrayList<>();
        NMF = new NextMoveFinder(model, beenToo,0,s,1);

    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        if (actions.isEmpty()){
            actions = NMF.getNextMoves();
            System.out.println(NMF.getBestMove().wentTo.size());
            for(int[] i:NMF.getBestMove().wentTo){
                beenToo[i[0]]+=1;
                if(i[0]>bestDist){
                    bestDist = i[0];
                    t = 0.5f;
                }
                else{
                    t += 0.2f;
                }
            }

            NMF = new NextMoveFinder(NMF.getBestMove().endModel,beenToo,bestDist,s,t);
        }

        return actions.removeFirst();
    }

    @Override
    public String getAgentName() {
        return "Michael Allen";
    }
}
