package andrew;

import battlecode.common.*;

public class Soldier extends Robot {
    // static int[][] memo;
    static MapLocation paintTower = null;
    static MapLocation wanderTarget = null;

    Soldier() {
        // memo = new int[mapHeight][mapWidth];
    }

    static MapLocation getTargetLoc() {
        if (rc.getPaint() < 80 && paintTower != null) {
            return paintTower;
        }
        if (wanderTarget == null) {
            wanderTarget = new MapLocation(rng.nextInt(0, mapWidth), rng.nextInt(0, mapHeight));
        }
        var curLoc = rc.getLocation();
        if (wanderTarget == curLoc) {
            wanderTarget = new MapLocation(rng.nextInt(0, mapWidth), rng.nextInt(0, mapHeight));
        }
        return wanderTarget;
    }

    @Override
    void play() throws GameActionException {
        var msgs = rc.readMessages(rc.getRoundNum() - 1);
        for (var msg : msgs) {
            paintTower = Communication.decodeTowerMessage(msg.getBytes());
            break;
        }
        var infos = rc.senseNearbyRobots();
        for (var info : infos) {
            if (info.type == UnitType.LEVEL_ONE_PAINT_TOWER && info.team == Globals.myTeam) {
                paintTower = info.location;
            }
        }
        var curLoc = rc.getLocation();
        var targetLoc = getTargetLoc();
        Logger.log("current location: " + curLoc);

        // Movement
        var dir = Movement.getMove(targetLoc);
        var endLoc = (dir != null && rc.canMove(dir) ? curLoc.add(dir) : curLoc);
        if (dir != null) {
            Movement.tryMove(dir);
            Logger.flush();
        }
        // After calling rc.move, the `rc.getLocation` does not change; it seems like all actions
        // are flushed when Clock.yield() is called.
        
        boolean paintedInFront = tryAttack(endLoc);
        if (paintedInFront) {
            var mapInfos = rc.senseNearbyMapInfos();
            for (var tile : mapInfos) {
                var loc = tile.getMapLocation();
                tryAttack(loc);
            }
        }
    }
    
    boolean tryAttack(MapLocation loc) throws GameActionException {
        if (rc.canPaint(loc) && rc.canAttack(loc) && !rc.senseMapInfo(loc).getPaint().isAlly()) {
            rc.attack(loc);
            return true;
        }
        return false;
    }
}