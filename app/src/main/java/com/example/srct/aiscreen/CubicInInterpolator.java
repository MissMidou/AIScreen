package com.example.srct.aiscreen;
import android.view.animation.Interpolator;


public class CubicInInterpolator implements Interpolator {
    private static final float PI = 3.14159265f;

    @Override
    public float getInterpolation(float input) {
        return input * input * input;
    }
}