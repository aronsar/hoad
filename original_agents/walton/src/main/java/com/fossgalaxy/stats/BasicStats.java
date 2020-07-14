package com.fossgalaxy.stats;

/**
 * Created by webpigeon on 26/01/17.
 */
public class BasicStats implements StatsSummary {
    private double min;
    private double max;
    private double sumSq;
    private double sum;
    private int n;

    public BasicStats() {
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.n = 0;
        this.sum = 0;
        this.sumSq = 0;
    }

    @Override
    public void add(double number) {
        this.min = Math.min(number, min);
        this.max = Math.max(number, max);
        this.sum += number;
        this.sumSq += number * number;
        this.n++;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getRange() {
        return max-min;
    }

    @Override
    public double getMean() {
        return sum/n;
    }

    public double getStdDev() {
        double top = sumSq - (n * getMean() * getMean());
        top /= n-1;
        return Math.sqrt(top);
    }


    public String toString() {
        return String.format("min: %f, max: %f, avg: %f, rng: %f ", getMin(), getMax(), getMean(), getRange());
    }
}
