package AUR.util.aslan; 

import AUR.util.knd.AURAreaGraph; 
import java.util.Comparator; 
 
/** 
 * 
 * @author Amir Aslan Aslani - Mar 2018 
 */ 
public class AURAreaCostComparator implements Comparator<AURAreaGraph> { 
 
        @Override 
        public int compare(AURAreaGraph o1, AURAreaGraph o2) { 
                return (int) ( o1.getTravelCost() - o2.getTravelCost() ); 
        } 
         
} 