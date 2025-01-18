package andrew;

import battlecode.common.*;

public class Tower extends Robot {
    static int numPaintTowers;
    static MapLocation[] paintTowers;

    Tower() {
        paintTowers = new MapLocation[25];
        numPaintTowers = 0;
        if (rc.getType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            paintTowers[numPaintTowers++] = rc.getLocation();
        }
    }

    @Override
    void play() throws GameActionException {
        MapLocation spawnLoc = rc.getLocation().add(Direction.SOUTH).add(Direction.SOUTH);
        if (rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)) {
            rc.buildRobot(UnitType.SOLDIER, spawnLoc);
            Logger.log("BUILT A SOLDIER");
        }
        
        if (numPaintTowers > 0) {
            var robots = rc.senseNearbyRobots();
            for (var robot : robots) {
                int index = rng.nextInt(numPaintTowers);
                if (rc.canSendMessage(robot.location)) {
                    rc.sendMessage(robot.location, Communication.encodeTowerMessage(paintTowers[index]));
                }
            }
        }
        // var messages = rc.readMessages(rc.getRoundNum() - 1);
        // for (var m : messages) {
        //     var loc = Communication.decodeTowerMessage(m.getBytes());
        //     // paintTowers
        // }
        // int endBytecodes = Clock.getBytecodeNum();

        Logger.flush();
    }

}