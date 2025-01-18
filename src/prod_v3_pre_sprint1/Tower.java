package prod_v3_pre_sprint1;

import java.util.Arrays;
import java.util.Optional;

import battlecode.common.*;

public class Tower extends Robot {

    static int soldiersProduced = 0;

    Tower() {
        super();
    }

    private boolean produceUnit(UnitType unitType) throws GameActionException {
        for (Direction dir : Direction.allDirections()) {
            MapLocation nextLoc = rc.getLocation().add(dir);
            if (rc.canBuildRobot(unitType, nextLoc)) {
                soldiersProduced++;
                rc.buildRobot(unitType, nextLoc);
                return true;
            }
        }
        return false;
    }
    
    @Override
    void play() throws GameActionException {
        boolean produceUnit = false;
        if (soldiersProduced < 2) produceUnit = true;
        else if (rc.getNumberTowers() > 4 && soldiersProduced < 10) produceUnit = true;
        else if (rc.getChips() >= 5000) produceUnit = true;

        if (produceUnit) {
            boolean produceSoldier = true;
            if (rng.nextInt(Math.max(rc.getNumberTowers() / 3, 1)) != 0) {
                produceSoldier = false;
            }
            if (produceSoldier) {
                produceUnit(UnitType.SOLDIER);
                soldiersProduced++;
            }
            else {
                produceUnit(UnitType.SPLASHER);
            }
        }

        // AOE attack
        rc.attack(null);

        // Single target attack lowest hp enemy in range
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(actionRadiusSquared, rc.getTeam().opponent());
        Optional<RobotInfo> target = Arrays.stream(nearbyEnemies).min((a, b) -> Integer.compare(a.getHealth(), b.getHealth()));
        if (target.isPresent()) {
            rc.attack(target.get().getLocation());
        }
    }

}