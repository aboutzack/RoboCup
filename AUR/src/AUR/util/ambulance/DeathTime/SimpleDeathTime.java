package AUR.util.ambulance.DeathTime;

import AUR.util.ambulance.Information.RescueInfo;

/**
 *
 * @author armanaxh - 2018
 */

public class SimpleDeathTime {

    public static int getDeathTimeInWorstCase(RescueInfo rescueInfo, int hp, int dmg) {

        int worstDmg = dmg - (rescueInfo.losDamge/2);
        int worstHp = hp + (rescueInfo.losHp/2);

        if(worstHp > 10000){
            worstHp = 10000;
        }
        if(worstDmg < 0 ){
            return RescueInfo.simulationTime;
        }

        int deathTime = rescueInfo.wsg.ai.getTime() + worstHp/worstDmg;

        return deathTime;
    }

}
