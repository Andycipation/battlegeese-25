package prod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import battlecode.common.*;

public class Soldier extends Robot {

    static MapLocation paintTowerAnchor = null;

    static MapLocation[] srpCenters = new MapLocation[1000];
    static int numSrpCenters = 0;

    static MapLocation targetSrpCenter = null;

    static boolean towerSoldier;

    Soldier() {
        super();
        towerSoldier = rng.nextInt(Math.max(2, rc.getNumberTowers() - 2)) == 0;
        for (int x = 0; x < mapWidth; ++x) {
            for (int y = 0; y < mapHeight; ++y) {
                if ((x + y * 3) % 10 == 0) {
                    srpCenters[numSrpCenters++] = new MapLocation(x, y);
                }
            }
        }
    }

    public static PaintType getPreferedPaintType(MapLocation loc) throws GameActionException {
        MapInfo tile = rc.senseMapInfo(loc);
        if (tile.getMark() != PaintType.EMPTY) {
            return tile.getMark();
        }
        boolean useSecondaryColor = false;
        if ((loc.x + loc.y) % 2 == 0 && (loc.x + loc.y * 3) % 10 != 0) {
            useSecondaryColor = true;
        }
        PaintType colorToPaint = (useSecondaryColor ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY);
        return colorToPaint;
    }

    public static boolean canBuildSrpBasedOnMemory(MapLocation loc) {
        int x = loc.x, y = loc.y;
        if (x < 2 || x >= mapWidth - 2 || y < 2 || y >= mapHeight - 2) return false;
        if (paintMemory[x-2][y-2].isEnemy() || unpaintable[x-2][y-2]) return false;
        if (paintMemory[x-2][y-1].isEnemy() || unpaintable[x-2][y-1]) return false;
        if (paintMemory[x-2][y-0].isEnemy() || unpaintable[x-2][y-0]) return false;
        if (paintMemory[x-2][y+1].isEnemy() || unpaintable[x-2][y+1]) return false;
        if (paintMemory[x-2][y+2].isEnemy() || unpaintable[x-2][y+2]) return false;
        if (paintMemory[x-1][y-2].isEnemy() || unpaintable[x-1][y-2]) return false;
        if (paintMemory[x-1][y-1].isEnemy() || unpaintable[x-1][y-1]) return false;
        if (paintMemory[x-1][y-0].isEnemy() || unpaintable[x-1][y-0]) return false;
        if (paintMemory[x-1][y+1].isEnemy() || unpaintable[x-1][y+1]) return false;
        if (paintMemory[x-1][y+2].isEnemy() || unpaintable[x-1][y+2]) return false;
        if (paintMemory[x-0][y-2].isEnemy() || unpaintable[x-0][y-2]) return false;
        if (paintMemory[x-0][y-1].isEnemy() || unpaintable[x-0][y-1]) return false;
        if (paintMemory[x-0][y-0].isEnemy() || unpaintable[x-0][y-0]) return false;
        if (paintMemory[x-0][y+1].isEnemy() || unpaintable[x-0][y+1]) return false;
        if (paintMemory[x-0][y+2].isEnemy() || unpaintable[x-0][y+2]) return false;
        if (paintMemory[x+1][y-2].isEnemy() || unpaintable[x+1][y-2]) return false;
        if (paintMemory[x+1][y-1].isEnemy() || unpaintable[x+1][y-1]) return false;
        if (paintMemory[x+1][y-0].isEnemy() || unpaintable[x+1][y-0]) return false;
        if (paintMemory[x+1][y+1].isEnemy() || unpaintable[x+1][y+1]) return false;
        if (paintMemory[x+1][y+2].isEnemy() || unpaintable[x+1][y+2]) return false;
        if (paintMemory[x+2][y-2].isEnemy() || unpaintable[x+2][y-2]) return false;
        if (paintMemory[x+2][y-1].isEnemy() || unpaintable[x+2][y-1]) return false;
        if (paintMemory[x+2][y-0].isEnemy() || unpaintable[x+2][y-0]) return false;
        if (paintMemory[x+2][y+1].isEnemy() || unpaintable[x+2][y+1]) return false;
        if (paintMemory[x+2][y+2].isEnemy() || unpaintable[x+2][y+2]) return false;
        return true;
    }

    public static boolean withinPattern(MapLocation center, MapLocation loc) {
        return Math.abs(loc.x - center.x) <= 2 && Math.abs(loc.y - center.y) <= 2;
    }

    @Override
    void play() throws GameActionException {
        // Sense information about all visible nearby tiles.
        RobotInfo[] nearbyEntities = rc.senseNearbyRobots();
        Logger.log(towerSoldier ? "tower soldier" : "not tower soldier");

        // If robot sees paint tower, set it to be paint anchor
        for (RobotInfo entity : nearbyEntities) {
            if (entity.getType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                paintTowerAnchor = entity.getLocation();
            }
        }

        // If robot has paint anchor, recharge to full
        if (paintTowerAnchor != null && getPaintProportion() < 0.5) {
            int paintNeeded = rc.getType().paintCapacity - rc.getPaint();
            if (rc.canTransferPaint(paintTowerAnchor, -paintNeeded)) {
                rc.transferPaint(paintTowerAnchor, -paintNeeded);
            }
            else if (!rc.getLocation().isAdjacentTo(paintTowerAnchor)) {
                BugNav.moveToward(paintTowerAnchor);
            }
        }
        else {
            if (towerSoldier) {
                boolean success = greedTower();
                if (!success) greedSrp();
            }
            else {
                greedSrp();
                if (rc.isActionReady()) greedyPaint();
                if (rc.isActionReady()) greedyAttack();
            }
            Logger.log(targetSrpCenter != null ? targetSrpCenter.toString() : "null");
        }
    }

    public static boolean greedTower() throws GameActionException {
        // Search for a nearby ruin to complete.
        MapLocation ruinLoc = null;
        for (MapLocation loc : rc.senseNearbyRuins(visionRadiusSquared)) {
            if (ruinLoc == null || rc.getLocation().distanceSquaredTo(ruinLoc) > rc.getLocation().distanceSquaredTo(loc)) {
                ruinLoc = loc;
            }
        }
        boolean ruinHasTower = false;
        if (ruinLoc != null) {
            for (RobotInfo entity : rc.senseNearbyRobots(ruinLoc, 0, myTeam)) {
                if (entity.getLocation().equals(ruinLoc)) {
                    ruinHasTower = true;
                }
            }
        }
        if (ruinLoc != null && !ruinHasTower){
            MapLocation targetLoc = ruinLoc;
            Direction dir = rc.getLocation().directionTo(targetLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
                Logger.log("Tower" + targetLoc);
            }
            // Mark the pattern we need to draw to build a tower here if we haven't already. Pick a random tower of {paint, money} to build.
            MapLocation shouldBeMarked = ruinLoc.subtract(dir);
            UnitType towerToMark = (rng.nextInt(2) == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
            if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToMark, targetLoc)){
                rc.markTowerPattern(towerToMark, targetLoc);
            }
            // Fill in any spots in the pattern with the appropriate paint.
            for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, actionRadiusSquared)){
                if (!withinPattern(targetLoc, patternTile.getMapLocation())) continue;
                if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY && !patternTile.getPaint().isEnemy()){
                    boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    Logger.log("here" + patternTile.getMapLocation());
                    if (rc.canAttack(patternTile.getMapLocation())) {
                        rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                        break;
                    }
                    else {
                        Direction delta = rc.getLocation().directionTo(patternTile.getMapLocation());
                        if (rc.canMove(delta)) rc.move(delta);
                        if (rc.canAttack(patternTile.getMapLocation())) {
                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                            break;
                        }
                    }
                }
            }
            // Complete the ruin if we can.
            UnitType[] towersToConsider = {UnitType.LEVEL_ONE_PAINT_TOWER, UnitType.LEVEL_ONE_MONEY_TOWER};
            for (UnitType towerType : towersToConsider) {
                if (rc.canCompleteTowerPattern(towerType, targetLoc)){
                    ruinHasTower = true;
                    rc.completeTowerPattern(towerType, targetLoc);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                }
            }
        }
        if (ruinLoc != null && ruinHasTower) {
            for (int dx = -GameConstants.PATTERN_SIZE / 2; dx < (GameConstants.PATTERN_SIZE + 1) / 2; dx++) {
                for (int dy = -GameConstants.PATTERN_SIZE / 2; dy < (GameConstants.PATTERN_SIZE + 1) / 2; dy++) {
                    MapLocation loc = ruinLoc.translate(dx, dy);
                    if (!rc.canSenseLocation(loc)) continue;
                    MapInfo tile = rc.senseMapInfo(ruinLoc.translate(dx, dy));
                    if (tile.getMark() != PaintType.EMPTY && rc.canRemoveMark(loc)) {
                        rc.removeMark(loc);
                    }
                }
            }
        }
        return (ruinLoc != null && !ruinHasTower);
    }

    public static void greedSrp() throws GameActionException {
        // if srp center is observed to be not valid, switch off it
        if (targetSrpCenter != null && !canBuildSrpBasedOnMemory(targetSrpCenter)) {
            targetSrpCenter = null;
        }

        // try to lock onto a new target center
        if (targetSrpCenter == null) {
            for (int it = 0; it < 13; ++it) {
                int idx = rng.nextInt(numSrpCenters);
                MapLocation loc = srpCenters[idx];
                if (!canBuildSrpBasedOnMemory(loc)) continue;
                if (targetSrpCenter == null || rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(targetSrpCenter)) {
                    targetSrpCenter = loc;
                }
            }
        }

        // if not new target center just randomly paint
        if (targetSrpCenter == null) {
            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
            for (MapInfo tile : nearbyTiles) {
                MapLocation loc = tile.getMapLocation();
                PaintType preferredPaintType = getPreferedPaintType(loc);
                if (rc.canPaint(loc) && rc.canAttack(loc) && tile.getPaint() != preferredPaintType) {
                    boolean useSecondaryColor = preferredPaintType == PaintType.ALLY_SECONDARY;
                    rc.attack(loc, useSecondaryColor);
                }
            }
        }
        else {
            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
            for (MapInfo tile : nearbyTiles) {
                MapLocation loc = tile.getMapLocation();
                if (!withinPattern(targetSrpCenter, loc)) continue;
                PaintType preferredPaintType = getPreferedPaintType(loc);
                if (rc.canPaint(loc) && rc.canAttack(loc) && tile.getPaint() != preferredPaintType) {
                    boolean useSecondaryColor = preferredPaintType == PaintType.ALLY_SECONDARY;
                    rc.attack(loc, useSecondaryColor);
                }
            }

            if (rc.canCompleteResourcePattern(targetSrpCenter)) {
                rc.setIndicatorDot(targetSrpCenter, 0, 0, 255);
                rc.completeResourcePattern(targetSrpCenter);
                targetSrpCenter = null;
            }
            else {
                BugNav.moveToward(targetSrpCenter);
                Logger.log("Srp");
                MapLocation newLoc = rc.getLocation();
                PaintType preferredPaintType = getPreferedPaintType(newLoc);
                if (rc.canPaint(newLoc) && rc.canAttack(newLoc) && rc.senseMapInfo(newLoc).getPaint() != preferredPaintType) {
                    boolean useSecondaryColor = getPreferedPaintType(newLoc) == PaintType.ALLY_SECONDARY;
                    rc.attack(newLoc, useSecondaryColor);
                }
            }
        }
    }

    public static void greedyPaint() throws GameActionException {
        // Paint any tile that this unit can see
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            MapLocation loc = tile.getMapLocation();
            PaintType preferredPaintType = getPreferedPaintType(loc);
            if (rc.canPaint(loc) && rc.canAttack(loc) && tile.getPaint() != preferredPaintType && !tile.getMark().isAlly()) {
                boolean useSecondaryColor = preferredPaintType == PaintType.ALLY_SECONDARY;
                rc.attack(loc, useSecondaryColor);
            }
        }
    }

    public static void greedyAttack() throws GameActionException {
        // Attack any tower that this unit can see
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            MapLocation loc = tile.getMapLocation();
            if (tile.hasRuin() && rc.canAttack(loc)) {
                rc.attack(loc);
            }
        }
    }
    
}
