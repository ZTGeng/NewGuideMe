package edu.sfsu.geng.newguideme.utils;

/**
 * Created by gengz on 3/31/17.
 */

public class Destination {

    private double lat;
    private double lng;
    private String name;

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getName() {
        return name;
    }

    public Destination(double lat, double lng, String name) {
        this.lat = lat;
        this.lng = lng;
        this.name = name;
    }

    public Destination(String des) {
        String[] questionPair = des.split("\\?", 2);
        name = questionPair[1];
        String[] commaPair = questionPair[0].substring(11).split(",");
        lat = Double.parseDouble(commaPair[0]);
        lng = Double.parseDouble(commaPair[1]);
    }

    @Override
    public String toString() {
        return "navigation:" + lat + "," + lng + "?" + name;
    }
}
