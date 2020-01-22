package mrl_2019.util;

import java.io.Serializable;

/**
 * User: roohola
 * Date: 3/25/11
 * Time: 3:47 PM
 */
public class PairSerialized<S, T> implements Serializable {
    static final long serialVersionUID = -198713768237652370L;
    private S first;
    private T second;

    public PairSerialized(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public PairSerialized() {
    }

    public S first() {
        return first;
    }

    public void setFirst(S first) {
        this.first = first;
    }

    public T second() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "PairSerialized{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        if (first == null && second != null) {
            hashCode = second.hashCode();
        } else if (second == null && first != null) {
            hashCode = first.hashCode();
        } else if (second == null && first == null)
            return hashCode;


        return first.hashCode() + second.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof PairSerialized) {
            PairSerialized p = (PairSerialized) o;

            if ((first != null && first.equals(p.first())) && (second != null && second.equals(p.second()))) {
                return true;
            } else {
                if (first == null && p.first() == null && (second != null && second.equals(p.second()))) {
                    return true;
                } else {
                    if (second == null && p.second() == null && (first != null && first.equals(p.first()))) {
                        return true;
                    } else {
                        if (first == null && p.first() == null && second == null && p.second() == null) {
                            return true;
                        }
                    }

                }
            }
        }
        return false;

    }
}
