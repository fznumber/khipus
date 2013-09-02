package com.encens.khipus.model.production;

import com.encens.khipus.model.employees.Month;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 31/08/13
 * Time: 12:07
 * To change this template use File | Settings | File Templates.
 */
public enum Periodo {
    FIRSTPERIODO("Periodo.first",1,15),
    SECONDPERIODO("Periodo.second",16,30);

    private int initDay;
    private int endDay;
    private String resourceKey;

    private Periodo(String resourceKey,int initDay, int endDay) {
        this.initDay = initDay;
        this.endDay = endDay;
        this.resourceKey = resourceKey;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public int getInitDay() {
        return initDay;
    }

    public void setInitDay(int initDay) {
        this.initDay = initDay;
    }

    public int getEndDay(int mount) {
        if((mount == 1 || mount == 3 || mount ==5 || mount ==7 || mount ==8 || mount ==10 || mount ==12) && (resourceKey == "Periodo.second"))
        return endDay +1;

        return endDay;
    }

    public int getEndDay() {

        return endDay;
    }

    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }
}