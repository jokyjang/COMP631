package project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.uncommons.maths.random.ExponentialGenerator;

public class ParameterGenerator {
	
	public static ParameterGenerator pg;
	
	public double delayMu;
	public double delaySigma;
	public double speedMu;
	public double speedSigma;
	public double lossRate;
	public double constraint;
	
	private ParameterGenerator(double dm, double ds, double sm, double ss, double lr, double ct) {
		this.delayMu = dm;
		this.delaySigma  = ds;
		this.speedMu = sm;
		this.speedSigma = ss;
		this.lossRate = lr;
		this.constraint = ct;
	}
	
	public static ParameterGenerator createPG(double dm, double ds, double sm, double ss, double lr, double ct) {
		if(pg == null)
			pg = new ParameterGenerator(dm, ds, sm, ss, lr, ct);
		return pg;
	}

	public static List<Double> logNormalGenerator(double scale, double shape, int size) {
		LogNormalDistribution lnd = new LogNormalDistribution(scale, shape);
		List<Double> list = new ArrayList<Double>(size);
		double[] sample = lnd.sample(size);
		for(double s : sample) {
			list.add(s);
		}
		return list;
	}

  public static List<Double> exponentialGenerator(int lambda, int size) {
    ExponentialGenerator rand =
        new ExponentialGenerator(lambda, new Random(System.currentTimeMillis()));
    List<Double> result = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      result.add(rand.nextValue());
    }
    return result;
  }
}
