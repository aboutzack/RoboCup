package AUR.util.aslan;

import AUR.util.knd.AURAreaGraph;
import java.util.Comparator;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURPoliceAreaScoreComparator implements Comparator<AURAreaGraph> {

        @Override
        public int compare(AURAreaGraph o1, AURAreaGraph o2) {
                return Double.compare(o2.getFinalScore(),o1.getFinalScore() );
        }
        
}
