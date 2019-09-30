package CCBBA.source;

import java.awt.*;
import java.util.Vector;

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

public class SubtaskBid {
    private double c_aj;                // self bid
    private double t_aj;                // subtask start time
    private Dimension x_aj;             // location of realization
    private int i_opt;                  // index of optimal path
    private double cost_aj;             // cost of task

    public SubtaskBid(){
        int inf = Integer.MIN_VALUE;
        this.c_aj = 0.0;
        this.t_aj = 0.0;
        this.x_aj = new Dimension(inf,inf);
        this.i_opt = 0;
        this.cost_aj = 0;
    }

    public void calcBidForSubtask(Subtask j, SimulatedAbstractAgent agent){
        Vector<Subtask> oldBundle = agent.getBundle();
        Vector<Subtask> oldPath = agent.getPath();

        double maxPathBid = Double.NEGATIVE_INFINITY;
        //double oldUtility = calcPathUtility(oldPath, agent);
        PathUtility oldUtility = calcPathUtility(oldPath, agent);
        Vector<Vector<Subtask>> possiblePaths = generateNewPaths(oldPath, j);

        Vector<Subtask> newBundle = new Vector<>();
        for(int i = 0; i < oldBundle.size(); i++) {
            newBundle.add(oldBundle.get(i));
        }
        newBundle.add(j);

        for (int i = 0; i < possiblePaths.size(); i++) { // Calculate utility for each new path
            Vector<Subtask> newPath = possiblePaths.get(i);
            //double newUtility = calcPathUtility(newPath, agent);
            PathUtility newUtility = calcPathUtility(newPath, agent);
            double newPathBid = newUtility.getUtility() - oldUtility.getUtility();
            double newPathCost = newUtility.getCost() - oldUtility.getCost();

            if(i != possiblePaths.size()-1){  // if ideal path modifies previously agreed order, deduct points
                newPathBid = newPathBid - 5.0;
            }

            //get max bid from all new paths
            if(newPathBid > maxPathBid){
                maxPathBid = newPathBid;
                this.c_aj = newPathBid;
                this.t_aj = calcTimeOfArrival(newPath, j, agent);
                this.x_aj = j.getParentTask().getLocation();
                this.i_opt = newPath.indexOf(j);
                this.cost_aj = newPathCost;
            }

        }
    }

    protected PathUtility calcPathUtility(Vector<Subtask> path, SimulatedAbstractAgent agent){
        PathUtility pathUtility = new PathUtility();
        PathUtility subtaskUtility = new PathUtility();
        //double pathUtility = 0.0;
        //double subtaskUtility;

        for(int i = 0; i < path.size(); i++){
            Subtask j = path.get(i);

            //Calculate time of arrival
            double t_a = calcTimeOfArrival(path, j, agent);

            // Calculate subtask utility within path
            subtaskUtility = calcSubtaskUtility(path,j,t_a, agent);

            // Add to path utility
            pathUtility.setUtility( pathUtility.getUtility() + subtaskUtility.getUtility() );
            pathUtility.setCost( pathUtility.getCost() + subtaskUtility.getCost() );
        }
        return pathUtility;
    }

    protected double calcTimeOfArrival(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        //Calculate time of arrival
        IterationResults localResults = agent.getLocalResults();
        double t_a;
        double t_corr;
        double t_quickest = calcQuickestArrivalTime(path, j, agent, localResults);

        Vector<Integer> timeConstraints = getTimeConstraints(j, path, agent);
        if(timeConstraints.size() != 0 ) { // if there are time constraints, consider them
            Task parentTask = j.getParentTask();
            double[][] T = parentTask.getT();
            double maxTz = Double.NEGATIVE_INFINITY;
            int i_max = 0;

            for (int i_j = 0; i_j < timeConstraints.size(); i_j++) {
                double thisTz = localResults.getTz().get( timeConstraints.get(i_j) );
                if( thisTz > maxTz ){
                    maxTz = thisTz;
                    i_max = timeConstraints.get(i_j);
                }
            }

            // looks to maximize utility by getting there as quick as possible
            t_corr = T[parentTask.getJ().indexOf(j)][parentTask.getJ().indexOf( localResults.getJ().get(i_max) )];
            t_a = maxTz - t_corr;

            if(t_a < t_quickest){ // if the
                t_a = t_quickest;
            }
        }
        else{
            t_a = t_quickest;
        }

        return t_a;
    }

    protected Vector<Vector<Subtask>> generateNewPaths(Vector<Subtask> oldPath, Subtask j){
        Vector<Vector<Subtask>> newPaths = new Vector<>();

        for(int i = 0; i < (oldPath.size()+1); i++){
            Vector<Subtask> tempPath = new Vector<>();
            for(int k = 0; k < oldPath.size(); k++){
                tempPath.add(oldPath.get(k));
            }
            tempPath.add(i,j);
            newPaths.add(tempPath);
        }

        return newPaths;
    }

    protected Vector<Integer> getTimeConstraints(Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        Vector<Integer> timeConstraints = new Vector<>();
        Task parentTask = j.getParentTask();
        int[][] D = parentTask.getD();

        // evaluate each dependent task
        for(int i = 0; i < parentTask.getJ().size(); i++){
            int i_j = parentTask.getJ().indexOf(j);
            int i_u = agent.getLocalResults().getJ().indexOf(parentTask.getJ().get(i));
            // if j depends on u and u has a winner, then add relationship to time constraints list
            if( ( D[i_j][i] == 1)&&(agent.getLocalResults().getZ().get(i_u) != null )&&(agent.getLocalResults().getZ().get(i_u) != agent ) ){
                timeConstraints.add( agent.getJ().indexOf(j) - parentTask.getJ().indexOf(j) + i );
            }
        }

        return timeConstraints;
    }

    protected double calcQuickestArrivalTime(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent, IterationResults localResults){
        double delta_x;
        Dimension x_i;
        int i = path.indexOf(j);
        double t_0;

        if(i == 0){
            x_i = agent.getInitialPosition();
            t_0 = j.getParentTask().getTC().get(0);
        }
        else{
            int i_prev = agent.getJ().indexOf( path.get(i-1) );
            x_i = path.get(i-1).getParentTask().getLocation();
            t_0 =  localResults.getTz().get(i_prev);
        }

        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);

        return sqrt(delta_x)/agent.getSpeed() + t_0;
    }

    private PathUtility calcSubtaskUtility(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        PathUtility pathUtility = new PathUtility();
        double S = calcSubtaskScore(path, j, t_a, agent);
        double g = calcTravelCost(path, j, agent);
        double p = calcMergePenalty(path, j, agent);
        double c_v = calcSubtaskCost(j); //<- UNCOMMENT WHEN VALIDATION IS DONE
//        double c_v = calcSubtaskCost(agent); // <- DELETE WHEN VALIDATION IS DONE

        pathUtility.setUtility(S - g - p - c_v);
        pathUtility.setCost(g + p + c_v);

        return pathUtility;
    }

    private double calcSubtaskScore(Vector<Subtask> path, Subtask j, double t_a, SimulatedAbstractAgent agent){
        double S_max = j.getParentTask().getS_max();
        double K = j.getK();
        double e = calcUrgency(j, t_a);
        double alpha = calcAlpha(j.getK(), j.getParentTask().getI());
        double sigmoid = calcSigmoid(path, j, agent);

        return (S_max/K)*e*alpha*sigmoid;
    }

    private double calcUrgency(Subtask j, double t_a){
        double lambda = j.getParentTask().getTC().get(3);
        double t_start = j.getParentTask().getTC().get(0);

//        return exp(- lambda * (t_a-t_start) );
        return 1.0; //<- UNCOMMENT WHEN VALIDATION STOPS
    }

    private double calcAlpha(double K, double I){
        if((K/I) == 1.0){
            return 1.0;
        }
        else return (1.0 / 3.0);
    }

    private double calcSigmoid(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        if(i == 0){
            Dimension x_i = agent.getInitialPosition();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }
        else{
            Dimension x_i = path.get(i-1).getParentTask().getLocation();
            Dimension x_f = j.getParentTask().getLocation();
            delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);
        }

        double gamma = j.getParentTask().getGamma();
        double distance =  sqrt(delta_x);
        double e = exp( gamma  * distance );
//        return 1.0/( 1 + e);
        return 1.0;
    }

    private double calcTravelCost(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double delta_x;
        Dimension x_i;
        if(i == 0){
            x_i = agent.getInitialPosition();
        }
        else{
            x_i = path.get(i-1).getParentTask().getLocation();
        }
        Dimension x_f = j.getParentTask().getLocation();
        delta_x = pow( (x_f.getHeight() - x_i.getHeight()) , 2) + pow( (x_f.getWidth() - x_i.getWidth()) , 2);

        double distance = sqrt(delta_x);

        return distance*agent.getMiu();
    }

    private double calcMergePenalty(Vector<Subtask> path, Subtask j, SimulatedAbstractAgent agent){
        int i = path.indexOf(j);
        double C_split = 0.0;
        double C_merge = 0.0;

        IterationResults localResults = agent.getLocalResults();
        Vector<Vector<SimulatedAbstractAgent>> omega = localResults.getOmega();
        if( i == 0){ // j is at beginning of path, no split penalty
            if(omega.get(i) != null) { // Is there a coalition at i?
                C_merge = agent.getC_merge();
            }
            else{
                C_merge = 0.0;
            }
            C_split = 0.0;
        }
        else {  // j has a previous subtask in the path
            if(omega.get(i-1) != null){ // Is there was a coalition at i - 1?
                if(omega.get(i) != null){ // Is there a coalition at i?
                    boolean sameCoalition = true;

                    //check if coalition is the same at i-1 and i
                    if(omega.get(i).size() == omega.get(i-1).size()){
                        for(int i_j = 0; i_j < omega.get(i).size(); i_j++){
                            if(!omega.get(i-1).contains( omega.get(i).get(i_j) )){
                                sameCoalition = false;
                                break;
                            }
                        }
                    }
                    else{
                        sameCoalition = false;
                    }

                    if(!sameCoalition){
                        C_split = agent.getC_split();
                        C_merge = agent.getC_merge();
                    }
                    else{
                        C_split = 0.0;
                        C_merge = 0.0;
                    }
                }
            }
            else{ // There was NO coalition at i-1
                if(omega.get(i) != null) { // Is there a coalition at i?
                    C_merge = agent.getC_merge();
                }
                else{
                    C_split = 0.0;
                    C_merge = 0.0;
                }
            }
        }
        return C_split + C_merge;
    }

    private double calcSubtaskCost(Subtask j){
        return j.getParentTask().getCost();
    }

    private double calcSubtaskCost(SimulatedAbstractAgent agent){
        return agent.readResources() * 1.14/100;
    }


    private double calcStartTime(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        double t_start = 0.0;

        return t_start;
    }

    private Dimension calcLocation(double local_bid, Subtask j, Vector<Subtask> path, SimulatedAbstractAgent agent){
        // Performs task at location of task.
        // IMPROVEMENT OPPORTUNITY: decide where to do task using sigmoid function
        Dimension location = j.getParentTask().getLocation();
        return location;
    }


    /**
     * Getters and setters
     */
    public double getC(){ return this.c_aj; }
    public double getTStart(){return this.t_aj;}
    public Dimension getX_aj(){return this.x_aj;}
    public int getI_opt(){return this.i_opt;}
    public double getCost_aj(){ return this.cost_aj; }
}
