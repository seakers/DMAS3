package CCBBA;

import java.util.Vector;

public class IterationResults {
    // Info used with other agents*********************
    private Vector<Double> y = new Vector<>();                       // winner bid list
    private Vector<SimulatedAbstractAgent> z = new Vector<>();       // winner agent list
    private Vector<Double> tz = new Vector<>();                      // arrival time list
    // *********************************************
    // Info used with self**************************
    private Vector<Double> c = new Vector<>();                       // self bid list
    private Vector<Integer> s = new Vector<>();                      // iteration stamp list
    private Vector<Integer> v = new Vector<>();                      // number of iterations in constraint violation
    private Vector<Integer> w_solo = new Vector<>();                 // permission to bid solo
    private Vector<Integer> w_any = new Vector<>();                  // permission to bid any
    // *********************************************

    public IterationResults(Vector<Subtask> J, int O_kq){
        int size = J.size();
        y.setSize(size);
        z.setSize(size);
        tz.setSize(size);

        c.setSize(size);
        s.setSize(size);
        y.setSize(size);
        w_solo.setSize(size);
        w_any.setSize(size);

        for(int i = 0; i < size; i ++){
            y.setElementAt(0.0, i);
            z.setElementAt(null, i);
            tz.setElementAt(0.0, i);

            c.setElementAt(0.0, i);
            s.setElementAt(0, i);
            v.setElementAt(0, i);
            w_solo.setElementAt(O_kq, i);
            w_any.setElementAt(O_kq, i);
        }
    }

    public IterationResults(IterationResults prevResults){
        // Copies results from previous iteration
        y = new Vector<>();
        z = new Vector<>();
        tz = new Vector<>();
        c = new Vector<>();
        s = new Vector<>();
        v = new Vector<>();
        w_solo = new Vector<>();
        w_any = new Vector<>();

        for(int i = 0; i < prevResults.getY().size(); i++){
            y.setElementAt(prevResults.getY().get(i), i );
            z.setElementAt(prevResults.getZ().get(i), i );
            tz.setElementAt(prevResults.getTz().get(i), i );
            c.setElementAt(prevResults.getC().get(i), i );
            s.setElementAt(prevResults.getS().get(i), i );
            v.setElementAt(prevResults.getV().get(i), i );
            w_solo.setElementAt(prevResults.getW_solo().get(i), i );
            w_any.setElementAt(prevResults.getW_any().get(i), i );
        }
    }

    /**
     * Getters and Setters
     */
    public Vector<Double> getY(){ return y; }
    public Vector<SimulatedAbstractAgent> getZ(){ return z; }
    public Vector<Double> getTz(){ return tz; }
    public Vector<Double> getC(){ return c; }
    public Vector<Integer> getS(){ return s; }
    public Vector<Integer> getV(){ return v; }
    public Vector<Integer> getW_solo(){ return w_solo; }
    public Vector<Integer> getW_any(){ return w_any; }

}