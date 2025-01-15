package prod;

import battlecode.common.*;

public class Splasher extends Robot {

    MapLocation targetEnemyTile = null;
    MapLocation targetUnexploredTile = null;

    Splasher() {
        super();
    }

    @Override
    void play() throws GameActionException {
        if (targetEnemyTile != null && !paintMemory[targetEnemyTile.x][targetEnemyTile.y].isEnemy())
            targetEnemyTile = null;
        if (targetUnexploredTile != null &&
            (paintMemory[targetUnexploredTile.x][targetUnexploredTile.y] != PaintType.EMPTY ||
             unpaintable[targetUnexploredTile.x][targetUnexploredTile.y]))
            targetUnexploredTile = null;

        if (targetEnemyTile == null)
            targetEnemyTile = findNewEnemyTile();
        if (targetUnexploredTile == null)
            targetUnexploredTile = findNewUnexploredTile();
            
        Logger.log("" + targetEnemyTile + ", " + paintMemory[targetEnemyTile.x][targetEnemyTile.y] + ", " + targetUnexploredTile);
        if (targetEnemyTile != null) {
            BugNav.moveToward(targetEnemyTile);
            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                MapLocation loc = tile.getMapLocation();
                if (tile.getPaint().isEnemy() && rc.canAttack(loc)) {
                    rc.attack(loc);
                }
            }
            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                MapLocation loc = tile.getMapLocation();
                if (tile.getPaint() == PaintType.EMPTY && rc.canAttack(loc) && !unpaintable[loc.x][loc.y]) {
                    rc.attack(loc);
                }
            }
        }
        else if (targetUnexploredTile != null) {
            BugNav.moveToward(targetUnexploredTile);
            if (rc.canAttack(targetUnexploredTile)) {
                rc.attack(targetUnexploredTile);
            }
        }
        else {
            // do nothing for now
        }
    }

    public static MapLocation findNewEnemyTile() {
        MapLocation enemyTile = null;
        MapLocation robotLocation = rc.getLocation();
        for (int i = 50; --i >= 0;) {
            MapLocation cur = randomMapLocation();
            if (paintMemory[cur.x][cur.y] == PaintType.EMPTY) {
                if (enemyTile == null || enemyTile.distanceSquaredTo(robotLocation) > cur.distanceSquaredTo(robotLocation)) {
                    enemyTile = cur;
                }
            }
        }
        return enemyTile;
    }

    public static MapLocation findNewUnexploredTile() {
        MapLocation unexploredTile = null;
        MapLocation robotLocation = rc.getLocation();
        for (int i = 50; --i >= 0;) {
            MapLocation cur = randomMapLocation();
            if (paintMemory[cur.x][cur.y] == PaintType.EMPTY) {
                if (unexploredTile == null || unexploredTile.distanceSquaredTo(robotLocation) > cur.distanceSquaredTo(robotLocation)) {
                    unexploredTile = cur;
                }
            }
        }
        return unexploredTile;
    }
    
}
