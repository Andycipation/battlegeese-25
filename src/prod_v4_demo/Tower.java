package prod_v4_demo;

import java.util.Arrays;
import java.util.Optional;

import battlecode.common.*;

public class Tower extends Robot {

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

        playSpawnUnits();
        playAttack();

    }

}