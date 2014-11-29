package project;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;

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
    this.delaySigma = ds;
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
    for (int i = 0; i < size; ++i) {
      double s = lnd.sample();
      list.add(s);
      System.out.println("Delay time for some peer is: " + s);
    }
    return list;
  }

  public List<Double> lossRatesGenerator(int size) {
    ExponentialDistribution rand = new ExponentialDistribution(this.lossRate);
    List<Double> peerLossRates = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      double lr = 1 - rand.density(rand.sample());
      peerLossRates.add(lr);
      System.out.println("Loss rate for some peer is: " + lr);
    }
    return peerLossRates;
  }
}
