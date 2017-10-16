package com.databazoo.devmodeler.tools;

/**
 * Collection of single-purpose math operations that are not supported by native library.
 */
public interface MathUtils {

    static boolean equalsPrecision3(double operand1, double operand2){
        return ((long) (operand1 * 1000.0)) == ((long) (operand2 * 1000.0));
    }

    static int min0(int val){
        return val < 0 ? 0 : val;
    }
}
