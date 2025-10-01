package agents.MichaelAllen;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class scoreValueMode{
    //                         V,   DX,DY,Jum,KIL,Mo,comp
    float[] data = new float[]{1.5f,1f,1f,10f,10f,2f,0f};
    float getVelocity(){
        return data[0];
    }
    float getDifXMode(){
        return data[1];
    }
    float getDifYMode(){
        return data[2];
    }
    float getJumpVal(){
        return data[3];
    }
    float getKillVal() {
        return data[4];
    }
    float getModeVal() {
        return data[5];
    }
    float getCompVal(){
        return data[6];
    }

}
class move{
    scoreValueMode s;
    List<move> kids;
    boolean[] getButtons(int x, int y){
        boolean[] out =new boolean[MarioActions.numberOfActions()];
        if(x > 0){
            out[MarioActions.RIGHT.getValue()] = true;
        }
        else if(x<0){
            out[MarioActions.LEFT.getValue()] = true;
        }
        if(y > 0){
            out[MarioActions.DOWN.getValue()] = true;
        }
        else if(y < 0){
            out[MarioActions.JUMP.getValue()] = true;
        }
        return out;
    }
    List<boolean[]> actions;
    MarioForwardModel endModel;
    public float lastScore;
    public float liveOdds = 1;
    public move bestKid;
    public List<int[]> wentTo;
    move(MarioForwardModel model,int x, int y,scoreValueMode s){
        this.s = s;
        wentTo = new ArrayList<>();
        float[] pos = model.getMarioFloatPos();
        pos[0]=(int) (pos[0]/16);
        pos[1]=(int) (pos[1]/16);
        boolean[] buttons = getButtons(x,y);
        int depth = 8;
        actions=new ArrayList<>();
        float[] lastPos={0,0};
        while(model.getGameStatus().equals(GameStatus.RUNNING)&&depth>0) {
            model.advance(buttons);
            actions.add(buttons.clone());
            float[] newPos = model.getMarioFloatPos();
            newPos[0] = (int) (newPos[0]/16);
            newPos[1] = (int) (newPos[1]/16);
            if (newPos[0] != lastPos[0]){
                wentTo.add(new int[]{(int)newPos[0], (int)newPos[1]});
            }
            lastPos=newPos;
            double dist = Math.sqrt(Math.pow(pos[0]-newPos[0],2)+Math.pow(pos[1]-newPos[1],2));
            if(dist > 2){
                break;
            }
            depth-=1;
        }
        endModel=model;
    }
    move getBestKid(MarioForwardModel model){
        float bestScore = -100000;
        if (kids!=null){
            int kidsScored = 0;
            float total = 0;
            if(kids.isEmpty()){
                kids = null;
                return null;
            }
            move bestKid = kids.getFirst();
            for(move k:kids){
                float sc = k.getScore(model,s);
                total += k.liveOdds;
                kidsScored++;
                if(sc>bestScore){
                    bestKid=k;
                    bestScore=sc;
                }
            }
            this.liveOdds = (float) Math.sqrt(total/kidsScored);//the less likely we are to live the less we like the move
            // but we root to becuse if the danger is far away we should not worry about it
            this.bestKid = bestKid;
            return bestKid;
        }

        else return null;
    }
    float getScore(MarioForwardModel model,scoreValueMode s){
        // if game State is GG
        float out = 0;
        if(endModel.getGameStatus().equals(GameStatus.LOSE)){
            this.liveOdds = 0;
            out = -100000;
        }
        else if(endModel.getGameStatus().equals(GameStatus.WIN)){
            out=100000-model.getMarioFloatPos()[1];// used to jump on the flag instead of just go it
        }
        else {
            float[] v = endModel.getMarioFloatVelocity();
            float[] oPos = model.getMarioFloatPos();
            float[] nPos = endModel.getMarioFloatPos();
            float[] dif = new float[]{nPos[0] - oPos[0], nPos[1] - oPos[1]};

            out += Math.abs(v[0])/s.getVelocity();// points for velocity not including y to stop just jumping
            out += dif[0] * s.getDifXMode();
            if(dif[1]<0)
                out -= dif[1]*s.getDifYMode();
            if(endModel.mayMarioJump())
                out += s.getJumpVal();
            out += (endModel.getKillsTotal()-model.getKillsTotal())*s.getKillVal();//points PerKill
            out += (endModel.getMarioMode()-model.getMarioMode())*s.getModeVal();//points PerKill
            out += nPos[0]*s.getCompVal();
            if (kids != null) {
                if (!kids.isEmpty()){
                    float kidScore=getBestKid(endModel).lastScore;
                    if (kidScore>90000)
                        out = kidScore - 10;
                    else
                        out += kidScore / 1.3f;
                }
            }
        }
        lastScore = out;
        return out;
    }
    void makeKids(){
        if (kids==null){
            if(endModel.getGameStatus().equals(GameStatus.RUNNING)){
                int[][] tileMap = endModel.getMarioSceneObservation();
                int[][] eniMap = endModel.getMarioEnemiesObservation();
                kids = new ArrayList<>();
                if(tileMap[8+1][8-1] == 0 && eniMap[8+1][8-1]==0){
                    kids.add(new move(endModel.clone(),1,-1,s));
                }
                if(tileMap[8-1][8-1] == 0 && eniMap[8-1][8-1]==0){
                    kids.add(new move(endModel.clone(),-1,-1,s));
                }
                if(tileMap[8+1][8+1] == 0 && eniMap[8+1][8+1]==0){
                    kids.add(new move(endModel.clone(),1,1,s));
                }
            }
        }
        else{
            for(move k: kids){
                k.makeKids();
            }
        }
    }
}
public class NextMoveFinder {
    Random r = new Random();
    scoreValueMode s;
    MarioForwardModel model;
    move BestMove;
    List<boolean[]> nextMoves;
    Thread nxt;
    int[] bt;
    int bestDist;
    boolean doRandomMove=false;
    NextMoveFinder(MarioForwardModel m,int[] beenToo,int bD,scoreValueMode s,float t){
        r.nextDouble(0,10f*t);
        if(r.nextDouble(0,10f*t)>5)//if we get stuck or have not made progress do moves more randomly
            doRandomMove = true;
        this.s =s;
        bestDist=bD;
        this.bt=beenToo;
        model=m;
        nxt = new Thread(this::FindNextMoves);
        nxt.start();
    }
    List<boolean[]> getNextMoves() {
        int t;
        while (nxt.isAlive()){
            try {
                Thread.sleep(6);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return nextMoves;
    }
    move getBestMove(){
        while (nxt.isAlive()){
            System.out.println("Waiting...");
        }
        return BestMove;
    }
    void FindNextMoves(){
        int depth = 6;
        int[][] tileMap = model.getMarioSceneObservation();
        int[][] eniMap = model.getMarioEnemiesObservation();
        List<move> options =new ArrayList<>();
        for(int x =-1; x<2 ;x++){
            for(int y =-1; y<2 ;y++){
                if(tileMap[8+x][8+y] == 0 && eniMap[8+x][8+y]==0){
                    options.add(new move(model.clone(),x,y,s));
                }
            }
        }
        for(int i=0;i<depth;i++){
            for (move m:options){
                m.makeKids();
            }
        }
        if (!doRandomMove){
            float bestScore = -100000;
            move bestMove = options.getFirst();
            System.out.println("___________________");
            System.out.println();
            for(move m:options){
                float sc = m.getScore(model,m.s);
                if(m.liveOdds < 0.8){
                    sc*= m.liveOdds;
                }
                System.out.print("score:"+sc);
                float[] ePos = m.endModel.getMarioFloatPos();
                int dist = (int)(ePos[0]/16);
                sc -= bt[(int)(ePos[0]/16)];
                if (dist<bestDist){
                    sc -= (float) Math.pow(bestDist-dist,2);
                }
                System.out.print(" bt:"+sc);
                System.out.println(" liveOdds:"+m.liveOdds);
                if(sc > bestScore){
                    bestMove = m;
                    bestScore=sc;
                }
            }
            this.BestMove = bestMove;
            this.nextMoves = bestMove.actions;
        }
        else {
            double total = 0;
            ArrayList<Float> opScores = new ArrayList<>();
            ArrayList<move> op = new ArrayList<>();
            System.out.println("_____________________");
            for (move m : options) {
                float sc = m.getScore(model, s);
                float[] ePos = m.endModel.getMarioFloatPos();
                int dist = (int) (ePos[0] / 16);
                sc -= bt[(int) (ePos[0] / 16)];
                if (dist < bestDist) {
                    sc -= (float) Math.pow(bestDist - dist, 2);
                }
                if (sc > 0) {
                    sc /= 20f;
                    sc = (float) Math.pow(sc, 5);
                    sc *= m.liveOdds;
                    opScores.add(sc);
                    op.add(m);
                    total += sc;
                }
                System.out.println("sc=" + sc);
            }
            if (op.isEmpty()) {
                BestMove = options.get(2);
                this.nextMoves = this.BestMove.actions;
            } else {
                double outR = r.nextDouble(0, total);
                int i = 0;
                while (!opScores.isEmpty() && outR > opScores.getFirst()) {
                    i++;
                    outR -= opScores.removeFirst();
                }
                this.BestMove = op.get(i);
                this.nextMoves = this.BestMove.actions;
            }
        }
    }
}
