package modules.spacecraft.instrument;

import modules.spacecraft.instrument.measurements.Measurement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import static java.lang.Math.pow;

public class Radiometer extends Instrument{
    private double nLooks;

    public Radiometer(String name, double dataRate, double pPeak, double pulseWidth, Measurement freq, double bandwidth, double n, double mass, String scanningType, double scanAnglePlus, double scanAngleMinus, String type, InstrumentAntenna ant, double nLooks) throws Exception {
        super(name, dataRate, pPeak, pulseWidth, freq, bandwidth, n, mass, scanningType, scanAnglePlus, scanAngleMinus, type, ant);
        this.nLooks = nLooks;
    }

    public Radiometer copy() throws Exception {
        return new Radiometer(this.name, this.dataRate, this.pPeak, this.pulseWidth, this.freq, this.bandwidth, this.n, this.mass, this.scanningType, this.scanAnglePlus, this.scanAngleMinus, this.type, this.ant, this.nLooks);
    }

    @Override
    public double getSNR(Measurement measurement, PVCoordinates satPV, PVCoordinates targetPV) throws Exception{
        Vector3D scPosition = satPV.getPosition();
        Vector3D targetPosition = targetPV.getPosition();
        double Aeff;
        if(this.ant.getType().equals("circular")){
            double r = this.ant.getDimAz()/2;
            double eff = this.ant.getEff();
            Aeff = eff * Math.PI * pow(r,2);
        }
        else{
            throw new Exception("Non-circular aperture antennas not yet supported");
        }
        double l = measurement.getLambda();
        double G = 4 * Math.PI * Aeff / pow(l,2);
        double Xr = getSpatialResEL(scPosition, targetPosition);
        double Xa = getSpatialResAZ(scPosition, targetPosition);
        double Gr = pulseWidth*bandwidth;

        double Re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double h = scPosition.getNorm() - Re;
        double V = satPV.getVelocity().getNorm();
        double V_ground = Re*V/scPosition.getNorm();
        double Ga = l*h/Xa/V_ground;

        double th = calcOffNadirAngle(scPosition, targetPosition);
        double area = nLooks*Gr*Ga*Xa*Xr*Math.cos(th)*dB2lin(sigma_dB);
        double r = targetPosition.subtract( scPosition ).getNorm();

        double T = estimateNoiseTemp(measurement);

        double kb=1.38e-23;
        double S = (pPeak*G/4/Math.PI/pow(r,2)) * area * (Aeff/4/Math.PI/pow(r,2));
        double SNR = S/(kb*T*bandwidth);
        return lin2dB(SNR);
    }

    @Override
    public double getSpatialResAZ(Vector3D scPosition, Vector3D targetPosition) {
        return getSpatialResEL(scPosition, targetPosition);
    }

    @Override
    public double getSpatialResEL(Vector3D scPosition, Vector3D targetPosition) {
        double th = calcOffNadirAngle(scPosition, targetPosition);
        double Re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double h = scPosition.getNorm() - Re;
        double dth = this.dtheta * Math.PI / 180;

        return h * ( Math.tan(th+dth/2.0) - Math.tan(th-dth/2.0) );
    }

}
