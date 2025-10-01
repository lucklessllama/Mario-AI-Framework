package agents.allen;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

public class Agent implements MarioAgent {
    PathFindToTile pf= new PathFindToTile();
    boolean[] action;
    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        action = new boolean[MarioActions.numberOfActions()];
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        pf.getBestMove(model);
        return pf.getBestMove(model);
    }

    @Override
    public String getAgentName() {
        return "Michael Allen";
    }
}
