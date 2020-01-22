package AIT_2019.module.complex;

import es.csic.iiia.bms.factors.CardinalityFactor;

public class BMSCardinalityFactor<T> extends CardinalityFactor<T>
{
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
    public String toString()
    {
        return "Cardinality[" + this.getIdentity() + "]";
    }

    private void reduce(T neighbor, boolean active)
    {
        if (active)
        {
            final CardinalityFunction func = this.getFunction();
            final CardinalityFunction next = new CardinalityFunction()
            {
                @Override
                public double getCost(int nActiveVariables)
                {
                    return func.getCost(++nActiveVariables);
                }
            };
            this.setFunction(next);
        }

        this.removeNeighbor(neighbor);
    }
}
