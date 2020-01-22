package CSU_Yunlu_2019.util.CSU_Ambulancehelper;

import rescuecore2.worldmodel.EntityID;

public class CSU_Target  {
        private EntityID victimID;
        private EntityID PositionID;
        private int distanceToRefuge;
        private int distanceToPartition;
        private int distanceToMe;
        private double victimSituation;

        private double cost;


        public CSU_Target(EntityID targetHumanID) {
            this.victimID = targetHumanID;
        }

        public EntityID getVictimID() {
            return victimID;
        }

        public EntityID getPositionID() {
            return PositionID;
        }

        public void setPositionID(EntityID positionID) {
            PositionID = positionID;
        }

        public int getDistanceToRefuge() {
            return distanceToRefuge;
        }

        public void setDistanceToRefuge(int distanceToRefuge) {
            this.distanceToRefuge = distanceToRefuge;
        }

        public int getDistanceToPartition() {
            return distanceToPartition;
        }

        public void setDistanceToPartition(int distanceToPartition) {
            this.distanceToPartition = distanceToPartition;
        }

        public int getDistanceToMe() {
            return distanceToMe;
        }

        public void setDistanceToMe(int distanceToMe) {
            this.distanceToMe = distanceToMe;
        }

        public double getVictimSituation() {
            return victimSituation;
        }

        public void setVictimSituation(double victimSituation) {
            this.victimSituation = victimSituation;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }
}
