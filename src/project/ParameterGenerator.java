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
	public int constraint;
	LogNormalDistribution speed;
	
	public ParameterGenerator(double dm, double ds, double sm, double ss, double lr, int ct) {
		this.delayMu = dm;
		this.delaySigma  = ds;
		this.speedMu = sm;
		this.speedSigma = ss;
		this.lossRate = lr;
		this.constraint = ct;
		speed = new LogNormalDistribution(sm, ss);
	}
	
	public double nextWaitTime() {
		return speed.sample();
	}

	public List<Double> delayGenerator(int size) {
		LogNormalDistribution lnd = new LogNormalDistribution(this.delayMu, this.delaySigma);
		List<Double> list = new ArrayList<Double>(size);
		double[] sample = lnd.sample(size);
		for(double s : sample) {
			list.add(s);
		}
		return list;
	}

  public List<Double> lossRatesGenerator(int size) {
    ExponentialGenerator rand =
        new ExponentialGenerator(this.lossRate, new Random(System.currentTimeMillis()));
    List<Double> peerLossRates = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      peerLossRates.add(1-rand.nextValue());
    }
    return peerLossRates;
  }
}
