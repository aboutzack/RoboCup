package mrl_2019.util;

import rescuecore2.misc.geometry.Line2D;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 7:46 PM
 */
public class MrlRay implements Serializable {
    static final long serialVersionUID = -198713768239652370L;
    private PairSerialized<PairSerialized<Double, Double>, PairSerialized<Double, Double>> ray;

    public MrlRay(Line2D ray) {
        this.ray = new PairSerialized<PairSerialized<Double, Double>, PairSerialized<Double, Double>>(new PairSerialized<Double, Double>(ray.getOrigin().getX(), ray.getOrigin().getY()), new PairSerialized<Double, Double>(ray.getEndPoint().getX(), ray.getEndPoint().getY()));
    }

    public Line2D getRay() {
        return new Line2D(ray.first().first(), ray.first().second(), ray.second().first(), ray.second().second());
    }

    public void setRay(Line2D ray) {
        this.ray = new PairSerialized<PairSerialized<Double, Double>, PairSerialized<Double, Double>>(new PairSerialized<Double, Double>(ray.getOrigin().getX(), ray.getOrigin().getY()), new PairSerialized<Double, Double>(ray.getEndPoint().getX(), ray.getEndPoint().getY()));
    }
}
