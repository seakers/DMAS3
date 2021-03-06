package modules.environment;

import modules.planner.CCBBA.IterationDatum;
import modules.spacecraft.instrument.measurements.Measurement;
import modules.spacecraft.instrument.measurements.MeasurementCapability;
import modules.spacecraft.instrument.measurements.MeasurementPerformance;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.exp;

public class TaskCapability {
    private final Task parentTask;
    private final HashMap<Measurement, MeasurementCapability> subtaskCapabilities;
    private ArrayList<ArrayList<MeasurementPerformance>> coalitionCapabilities;
    private ArrayList<ArrayList<IterationDatum>> coalitionBids;

    // Requirement Satisfaction Parameters
    private double spatRes;
    private double snr;
    private double revTime;
    private double resSat;
    private double snrSat;
    private double revisitSat;

    public TaskCapability(Task task){
        parentTask = task;

        subtaskCapabilities = new HashMap<>();
        for(Measurement measurement : task.getMeasurements()){
            MeasurementCapability measurementCapability = new MeasurementCapability(measurement);
            subtaskCapabilities.put(measurement, measurementCapability);
        }

        coalitionCapabilities = new ArrayList<>();
        coalitionBids = new ArrayList<>();

        spatRes = -1.0;
        snr = -1.0;
        revTime = -1.0;
        resSat = -1.0;
        snrSat = -1.0;
        revisitSat = -1.0;
    }

    public void calcReqSat() throws Exception {
        coalitionCapabilities = calcCoalitionCapabilities();

        resSat = calcResSat();
        snrSat = calcSNRSat();
        revisitSat = calcRevisitSat();
    }

    private ArrayList<ArrayList<MeasurementPerformance>> calcCoalitionCapabilities(){
        ArrayList<ArrayList<MeasurementPerformance>> coalCaps = new ArrayList<>();
        ArrayList<ArrayList<IterationDatum>> coalBids = new ArrayList<>();

        Dependencies dep = parentTask.getDependencies();

        for(Measurement measurement : parentTask.getMeasurements()){
            MeasurementCapability measurementCapability = subtaskCapabilities.get(measurement);
            ArrayList<IterationDatum> bids = measurementCapability.getPlannerBids();
            ArrayList<MeasurementPerformance> perfs = measurementCapability.getPerformance();

            for(int i = 0; i < perfs.size(); i++){
                Subtask j = bids.get(i).getSubtask();
                IterationDatum bid_j = bids.get(i);
                AbsoluteDate date_j = bid_j.getTz();

                boolean coalFound = false;
                for(ArrayList<IterationDatum> coal : coalBids){

                    for(IterationDatum bid_q : coal){
                        Subtask q = bid_q.getSubtask();
                        AbsoluteDate date_q = bid_q.getTz();

                        if(dep.depends(j,q) &&
                            dep.Tmax(j,q) >= Math.abs( date_j.durationFrom(date_q) ) &&
                            dep.Tmin(j,q) <= Math.abs( date_j.durationFrom(date_q) )){
                            // j and q are part of the same measurement
                            coalFound = true;
                        }
                        else coalFound = false;
                    }

                    if(coalFound){
                        coal.add(bid_j);
                        coalCaps.get(coalBids.indexOf(coal)).add(perfs.get(i));
                        break;
                    }
                }

                if(coalBids.size() == 0 || !coalFound){
                    ArrayList<MeasurementPerformance> tempCoal = new ArrayList<>();
                    tempCoal.add(perfs.get(i));

                    ArrayList<IterationDatum> tempCoalBid = new ArrayList<>();
                    tempCoalBid.add(bids.get(i));

                    coalCaps.add(tempCoal);
                    coalBids.add(tempCoalBid);
                }
            }
        }

        this.coalitionBids = coalBids;
        return coalCaps;
    }

    private double calcResSat() throws Exception {
        double spatialResAvg = -1.0;
        double n = 0.0;

        for(ArrayList<MeasurementPerformance> cap : coalitionCapabilities){
            double res = 1.0;
            int n_res = 0;
            for(MeasurementPerformance perf : cap){
                res *= perf.getSpatialRes();
                n_res++;
            }

            if(res != 1.0){
                res = Math.pow(res, 1/((double) n_res));
                if(n == 0.0) spatialResAvg = res;
                else spatialResAvg += res;
                n += 1.0;
            }
        }

        if(Math.abs(spatialResAvg) < 0.0) return 0.0;
        else if(n == 0) return 0.0;
        spatialResAvg = spatialResAvg/n;

        spatRes = spatialResAvg;

        Requirements req = parentTask.getRequirements();
        double spatialResReq = req.getSpatialResReq();
        double spatialResReqSlope = req.getSpatialResReqSlope();

        double delta_spat = spatialResAvg - spatialResReq;
        double e_spat = exp(spatialResReqSlope * delta_spat);
        double sigmoid_spat;
        if(e_spat == Double.POSITIVE_INFINITY){
            sigmoid_spat = 0.0;
        }
        else if(e_spat == Double.NEGATIVE_INFINITY){
            sigmoid_spat = 1.0;
        }
        else{
            sigmoid_spat = ( 1.0 /( 1.0 + e_spat));
        }

        return sigmoid_spat;
    }

    private double calcSNRSat() throws Exception {
        double snrAvg = 0.0;
        double n = 0.0;
        for(Measurement measurement : parentTask.getMeasurements()){
            MeasurementCapability measurementCapability = subtaskCapabilities.get(measurement);

            for(MeasurementPerformance performance : measurementCapability.getPerformance()){
                snrAvg += performance.getSNR();
                n += 1.0;
            }
        }

        if(Math.abs(n) < 1e-3) return 0.0;
        else if(n == 0) return 0.0;
        snrAvg = snrAvg/n;

        snr = snrAvg;

        Requirements req = parentTask.getRequirements();
        double snrReq = req.getLossReq();
        double snrReqSlope = req.getLossReqSlope();

        double delta_snr = snrReq - snrAvg;
        double e_snr = exp(snrReqSlope * delta_snr);
        double sigmoid_snr;
        if(e_snr == Double.POSITIVE_INFINITY){
            sigmoid_snr = 0.0;
        }
        else if(e_snr == Double.NEGATIVE_INFINITY){
            sigmoid_snr = 1.0;
        }
        else{
            sigmoid_snr = ( 1.0 /( 1.0 + e_snr));
        }

        return sigmoid_snr;
    }

    private double calcRevisitSat(){
        ArrayList<AbsoluteDate> measurementDates = new ArrayList<>();
        measurementDates = getMeasurementDates();

        double revisitTimeAvg = 0.0;
        int n = 0;
        for(int i = 0; i < measurementDates.size(); i++){
            if(i == 0){
                continue;
            }

            AbsoluteDate date_i = measurementDates.get(i);
            AbsoluteDate date_j = measurementDates.get(i-1);

            revisitTimeAvg = revisitTimeAvg + date_i.durationFrom(date_j);
            n++;
        }

        if(n < 1 || revisitTimeAvg == 0.0) return 0.0;
        revisitTimeAvg = revisitTimeAvg/n;

        revTime = revisitTimeAvg;

        Requirements req = parentTask.getRequirements();
        double t_max = req.getTemporalResolutionMax();
        double t_min = req.getTemporalResolutionMin();

        double alpha_min = Math.log( (1.0/0.99) - 1 );
        double alpha_max = Math.log( (1.0/0.01) - 1 );

        double t_0 = (t_min - (alpha_min/alpha_max)* t_max) / (1 - alpha_min/alpha_max);
        double lambda = alpha_max/(t_max-t_0);

        double dt = revisitTimeAvg - t_0;
        double e_rev = exp(lambda * dt);
        double sigmoid_rev;
        if(e_rev == Double.POSITIVE_INFINITY){
            sigmoid_rev = 0.0;
        }
        else if(e_rev == Double.NEGATIVE_INFINITY){
            sigmoid_rev = 1.0;
        }
        else{
            sigmoid_rev = ( 1.0 /( 1.0 + e_rev));
        }

        return sigmoid_rev;
    }

    private ArrayList<AbsoluteDate> getMeasurementDates(){
        ArrayList<AbsoluteDate> dates = new ArrayList();
        ArrayList<AbsoluteDate> datesSorted = new ArrayList();

        for(Measurement measurement : this.getCapabilities().keySet()){
            MeasurementCapability measurementCapability_j = this.getCapabilities().get(measurement);

            for(IterationDatum datum_j : measurementCapability_j.getPlannerBids()){
                // if j has dependent measurements, see if they were performed by someone else
                AbsoluteDate date = datum_j.getTz().getDate();
                dates.add(date);
            }
        }

        while(dates.size() > 0) {
            AbsoluteDate minDate = null;
            for (AbsoluteDate date : dates) {
                if(minDate == null) minDate = date;
                else if(date.compareTo(minDate) <= 0){
                    minDate = date.getDate();
                }
            }

            datesSorted.add(minDate.getDate());
            dates.remove(minDate);
        }

        return datesSorted;
    }

    public void updateSubtaskCapability(MeasurementCapability newCapability){
        Measurement measurement = newCapability.getMeasurement();
        subtaskCapabilities.get(measurement).update(newCapability);
    }

    public MeasurementCapability getSubtaskCapability(Subtask subtask){ return this.subtaskCapabilities.get(subtask.getMainMeasurement());}
    public HashMap<Measurement, MeasurementCapability> getCapabilities(){return this.subtaskCapabilities;}
    public Task getParentTask(){return parentTask;}

    public double getResSat() { return resSat; }
    public double getSnrSat() { return snrSat; }
    public double getRevSat() { return revisitSat; }
    public double getSpatRes(){ return spatRes;}
    public double getSnr()    { return snr;}
    public double getRevTime(){ return revTime;}
    public ArrayList<ArrayList<MeasurementPerformance>> getCoalitionCapabilities(){return  this.coalitionCapabilities;}
    public ArrayList<ArrayList<IterationDatum>> getCoalitionBids(){return this.coalitionBids;}
}
