package CSU_Yunlu_2020.world.object;


import rescuecore2.worldmodel.EntityID;

public class CSUHydrant {
	private EntityID selfID;
	private boolean occupied;

	public CSUHydrant(EntityID id) {
		this.selfID = id;
		this.occupied  = false;
	}
    /**
     * must  be overwrite if used
     */
	public void update() {
		this.occupied = ! this.occupied;
	}
	
	public boolean isOccuped() {
		return this.occupied;
	}
	
	public String toString() {
		return "CSUH[" + this.selfID +"]";
	}

}
