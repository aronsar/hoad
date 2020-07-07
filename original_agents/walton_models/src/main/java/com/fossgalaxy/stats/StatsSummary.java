package com.fossgalaxy.stats;

/**
 * Created by webpigeon on 26/01/17.
 */
public interface StatsSummary {

    double getMax();
    double getMin();
    double getRange();
    double getMean();

    void add(double score);

    int getN();
}
