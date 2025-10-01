package agents.allen;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.ArrayList;


class move{
    move[] kids;
    boolean[] actions;
    MarioForwardModel model;
    public boolean inUse = true;
    public ArrayList<boolean[]> foundGoal(int[] goal){
        ArrayList<boolean[]> out;
        if (kids == null) {
            if(!inUse){
                return null;
            }
            float[] pos = model.getMarioFloatPos();
            if ((int) pos[0] == goal[0] && (int) pos[1] == goal[1]) {
                out = new ArrayList<>();
                out.add(actions);
                return out;
            } else {
                return null;
            }
        }
        else{
           ArrayList<boolean[]> kidOut;
            for(move i:kids){
                kidOut = i.foundGoal(goal);
                if(kidOut!=null){
                    kidOut.add(actions);
                    return kidOut;
                }
            }
            return null;
        }
    }
    public void makeKids(int[] goal , move[][] found, float[] pos1){
        if (kids == null){
            if(inUse && !model.getGameStatus().toString().equals("LOSE")){
                System.out.println("KIDS made");
                move[] options = new move[5];
                float[] pos = model.getMarioFloatPos();
                boolean[] ac = new boolean[5];
                ac[MarioActions.JUMP.getValue()] = true;
                ac[MarioActions.RIGHT.getValue()] = true;
                options[0] = new move(model,ac.clone(),found,pos);
                ac = new boolean[5];
                ac[MarioActions.JUMP.getValue()] = true;
                ac[MarioActions.LEFT.getValue()] = true;
                options[1] = new move(model,ac.clone(),found,pos);
                ac = new boolean[5];
                ac[MarioActions.RIGHT.getValue()] = true;
                options[2] = new move(model,ac.clone(),found,pos);
                ac = new boolean[5];
                ac[MarioActions.LEFT.getValue()] = true;
                options[3] = new move(model,ac.clone(),found,pos);
                ac = new boolean[5];
                ac[MarioActions.JUMP.getValue()] = true;
                options[4] = new move(model,ac.clone(),found,pos);
                this.kids= options;
            }
        }
        else if (!model.getGameStatus().toString().equals("LOSE")){
            for (move i: kids){
                i.makeKids(goal,found,pos1);
            }
        }
    }
    public float getDist(int[] goal){
        float bestDist= 1000;
        if (kids!=null){
            for (move i:kids){
                float d = i.getDist(goal);
                if (d < bestDist){
                    bestDist = d;
                }
            }
        }
        else{
            if(model.getGameStatus().equals(GameStatus.LOSE) || !inUse ){
                return 10000;
            }
            float[] p = model.getMarioFloatPos();
            p[0] /= 16;
            p[1] /= 16;
            bestDist =(float) Math.sqrt(Math.pow(p[0]-goal[0],2)+Math.pow(p[1]-goal[1],2));
        }
        return bestDist;
    }
    move(MarioForwardModel model,boolean[] actions,move[][] found, float[] mPos){
        this.actions = actions;
        float[] lPos = model.getMarioFloatPos();
        this.model =  model.clone();
        this.model.advance(actions);
        float[] pos = this.model.getMarioFloatPos();
        int d =12;
        while ((int)pos[0] == (int)lPos[0] && (int)pos[1] == (int)lPos[1] && d>0){
            if (model.getGameStatus().equals(GameStatus.LOSE))
                break;
            this.model.advance(actions);
            pos = this.model.getMarioFloatPos();
            d--;
        }
        if (!model.getGameStatus().equals(GameStatus.LOSE)) {
            int x = 8 + (int) (mPos[0] / 16) - (int) (pos[0] / 16);
            int y = 8 + (int) (mPos[1] / 16) - (int) (pos[1] / 16);
            if (found[x][y] != null) {
                this.inUse = false;
            } else found[x][y] = this;
        }
        else this.inUse=false;
    }
}

public class PathFindToTile {
    boolean[] getMoveScore(MarioForwardModel model, int[] goal){
        move[][] found = new move[16][16];

        move[] options = new move[5];
        float[] pos = model.getMarioFloatPos();
        boolean[] ac = new boolean[5];
        ac[MarioActions.JUMP.getValue()] = true;
        ac[MarioActions.RIGHT.getValue()] = true;
        options[0] = new move(model,ac.clone(),found,pos);
        ac = new boolean[5];
        ac[MarioActions.JUMP.getValue()] = true;
        ac[MarioActions.LEFT.getValue()] = true;
        options[1] = new move(model,ac.clone(),found,pos);
        ac = new boolean[5];
        ac[MarioActions.RIGHT.getValue()] = true;
        options[2] = new move(model,ac.clone(),found,pos);
        ac = new boolean[5];
        ac[MarioActions.LEFT.getValue()] = true;
        options[3] = new move(model,ac.clone(),found,pos);
        ac = new boolean[5];
        ac[MarioActions.JUMP.getValue()] = true;
        options[4] = new move(model,ac.clone(),found,pos);
        float d = 16;
        while (d>0){
            d--;
            for (move move : options) {
                if (move.foundGoal(goal) != null) {
                    //System.out.println("Found");
                    return move.actions;
                }
            }
            for (move option : options) {
                //System.out.println("what up");
                option.makeKids(goal, found, pos);
            }
        }
        System.out.println("notFound");
        float bestDist = 10000;
        int best=0;
        for(int i =0; i<options.length;i++){
            float dist = options[i].getDist(goal);
            if(bestDist > dist){
                best = i;
                bestDist = dist;
            }
        }
        System.out.println(bestDist);
        return options[best].actions;
    }
    int[] getGoal(MarioForwardModel model){
        int[][] map = model.getMarioSceneObservation();
        int[] goal = new int[2];
        boolean endOfGround;
        int h = 16;
        for(int x=0; x<16;x++){
            endOfGround=false;
            if(x>8 && goal[0]<x-1){
                break;
            }
            for(int y = 15; y>=0; y--){
                if (x > 8){
                    if ((y==0||map[x][y-1]==0 )&& !endOfGround){
                        endOfGround=true;
                        if ( h >= y)
                            h=y;
                        else if (h<8){
                            x=16;
                            break;
                        }
                    }
                }
                if (y==0 && map[x][y]!=0){
                    goal[0]=x;
                    goal[1]=y;
                }
                else if (y==1 && map[x][y]!=0 && map[x][y-1]==0){
                    goal[0]=x;
                    goal[1]=y;
                }
                else if(map[x][y]!=0 && map[x][y-1]==0 && map[x][y-2]==0){
                    goal[0]=x;
                    goal[1]=y;
                }
            }
            if(x<16)
                if(map[x][4]!=0&& map[x][3]==0){
                    goal[0] = x;
                    goal[1] = 4;
                }
        }
        goal[0] += (int)(model.getMarioFloatPos()[0]/16)-8;
        goal[1] += (int)(model.getMarioFloatPos()[1]/16)-8;
        return goal;
    }
    boolean[] getBestMove(MarioForwardModel model){
        int[] goal = getGoal(model);
        System.out.println("Goal:x,"+goal[0]+": y,"+goal[1]);
        float[] pos = model.getMarioFloatPos();
        pos[0]/=16;
        pos[1]/=16;
        System.out.println("Pos:x,"+pos[0]+": y,"+pos[1]);
        getMoveScore(model,goal);
        return getMoveScore(model,goal);
    }
}
