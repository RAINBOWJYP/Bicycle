package com.example.jyp2.bicyclemanager;

import android.widget.EditText;

/**
 * Created by jyp on 2017-06-08.
 */

public class Calculation {


    public static double Calculate(int thick, double body, double bike){
        double proper = 0;
        switch (thick){
            case 20: proper = (0.33*2.2*(body+bike))+63.33;
                return proper;
            case 23: proper = (0.33*2.2*(body+bike))+53.33;
                return proper;
            case 25: proper = (0.33*2.2*(body+bike))+43.33;
                return proper;
            case 28: proper = (0.33*2.2*(body+bike))+33.33;
                return proper;
            case 32: proper = (0.33*2.2*(body+bike))+31.67;
                return proper;
            case 37: proper = (0.33*2.2*(body+bike))+26.67;
                return proper;
        }
        return proper;
    }

    public static double Calculate(double body, double bike){
        double proper =0;
        proper = (0.33*2.2*(body+bike))+63.33;

        return proper;
    }
}
