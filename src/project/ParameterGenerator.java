package project;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.uncommons.maths.random.ExponentialGenerator;

public class ParameterGenerator {
  public static List<Double> logNormalGenerator(int meanValue, int size) {
    Random rand = new Random(System.currentTimeMillis());
    List<Double> result = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      result.add(Math.exp(rand.nextDouble()));
    }
    return result;
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
