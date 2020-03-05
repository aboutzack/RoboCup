package CSU_Yunlu_2019.module.complex.fb;

import rescuecore2.worldmodel.EntityID;

/**
* @Description: interface for target selector
* @Author: Guanyu-Cai
* @Date: 3/5/20
*/
public interface IFireBrigadeTargetSelector {
    /**
     * @return target
     */
    public EntityID calc();
}
