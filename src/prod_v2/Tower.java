package prod_v2;

import java.util.Arrays;
import java.util.Optional;

import battlecode.common.*;

public class Tower extends Robot {

    static int soldiersProduced = 0;

    Tower() {
        super();
    }
    
    @Override
    void play() throws GameActionException {
        boolean produceSoldier = soldiersProduced < 2 || (rc.getNumberTowers() > 4 && soldiersProduced < 15);
        if (produceSoldier) {
            for (Direction dir : Direction.allDirections()) {
                MapLocation nextLoc = rc.getLocation().add(dir);
                if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
                    soldiersProduced++;
                    rc.buildRobot(UnitType.SOLDIER, nextLoc);
                    break;
                }
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