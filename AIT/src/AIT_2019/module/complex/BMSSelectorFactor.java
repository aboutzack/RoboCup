package AIT_2019.module.complex;

import es.csic.iiia.bms.MaxOperator;
import es.csic.iiia.bms.factors.SelectorFactor;
import java.util.*;

public class BMSSelectorFactor<T> extends SelectorFactor<T>
{
    private T fixedneighbor = null;

    @Override
    public long run()
    {
        if (this.fixedneighbor == null) return super.run();

        MaxOperator op = this.getMaxOperator();
        final Collection<T> neighbors = this.getNeighbors();

        for (T neighbor : neighbors)
        {
            if (neighbor.equals(this.fixedneighbor)) op = op.inverse();
            this.send(op.getWorstValue(), neighbor);
        }
        return neighbors.size();
    }

    @Override
    public void receive(double message, T sender)
    {
        if (!Double.isInfinite(message))
        {
            super.receive(message, sender);
            return;
        }

        if (this.getNeighbors().contains(sender))
        {
            final double worst = this.getMaxOperator().getWorstValue();
            this.reduce(sender, worst != message);
            this.send(message, sender);
        }
    }

    @Override
    public T select()
    {
        return this.fixedneighbor == null ? super.select() : this.fixedneighbor;
    }

    @Override
    public String toString()
    {
        return "Selector[" + this.getIdentity() + "]";
    }

    private void reduce(T neighbor, boolean active)
    {
        if (active) this.fixedneighbor = neighbor;
        this.removeNeighbor(neighbor);

        if (this.fixedneighbor == null)
        {
            final List<T> neighbors = this.getNeighbors();
            final int nRemainings = neighbors.size();
            if (nRemainings == 1) this.fixedneighbor = neighbors.get(0);
        }
    }
}
