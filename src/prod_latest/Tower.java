package prod_latest;

import battlecode.common.*;

public class Tower extends Robot {

    public static SpawnStrategy spawnStrat;
    public static int unitsBuilt = 0;

    public static boolean tryBuildUnit(UnitType robotType) throws GameActionException {
        for (int i = adjacentDirections.length; --i >= 0; ) {
            MapLocation loc = curLoc.add(adjacentDirections[i]);
            if (rc.canBuildRobot(robotType, loc)) {
                rc.buildRobot(robotType, loc);
                unitsBuilt++;
                return true;
            }
        }
        return false;
    }

    public static void playSpawnUnits() throws GameActionException {
        // demo of logger
        Logger.log("Units built: " + unitsBuilt);
        spawnStrat.act();
    }

    public static void playAttack() throws GameActionException {
        // AOE attack
        rc.attack(null);

        // Single target attack lowest hp enemy in range
        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            MapLocation loc = nearbyEnemyRobots[i].getLocation();
            if (rc.canAttack(loc)) {
                rc.attack(loc);
                break;
            }
        }
    } 
    
    @Override
    void play() throws GameActionException {

        if (spawnStrat == null) {
            switch (mapCategory) {
                case SIZE1 -> spawnStrat = new Size1MapSpawnStrategy();
                case SIZE2 -> spawnStrat = new Size2MapSpawnStrategy();
                case SIZE3 -> spawnStrat = new Size3MapSpawnStrategy();
            }
        }

        playSpawnUnits();
        playAttack();

    }

    static abstract class SpawnStrategy extends Tower {
        abstract public void act() throws GameActionException;
    }

    static class Size1MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 15;
                int splasherWeight = 3 * numTowers;
                int mopperWeight = 2 * numTowers;
                UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                    case 0 -> UnitType.SOLDIER;
                    case 1 -> UnitType.SPLASHER;
                    default -> UnitType.MOPPER;
                };
                tryBuildUnit(unitPicked);
            }
        }
    }

    static class Size2MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 16;
                int splasherWeight = numTowers;
                int mopperWeight = numTowers;
                UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                    case 0 -> UnitType.SOLDIER;
                    case 1 -> UnitType.SPLASHER;
                    default -> UnitType.MOPPER;
                };
                tryBuildUnit(unitPicked);
            }
        }
    }

    static class Size3MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 20;
                int splasherWeight = numTowers;
                int mopperWeight = numTowers;
                UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                    case 0 -> UnitType.SOLDIER;
                    case 1 -> UnitType.SPLASHER;
                    default -> UnitType.MOPPER;
                };
                tryBuildUnit(unitPicked);
            }
        }
    }

}
