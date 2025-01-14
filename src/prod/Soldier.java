package prod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import battlecode.common.*;

public class Soldier extends Robot {

    static MapLocation paintTowerAnchor = null;

    static MapLocation lastLocation = null;

    @Override
    void play() throws GameActionException {
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        RobotInfo[] nearbyEntities = rc.senseNearbyRobots();
        boolean paintLeqHalfFull = rc.getPaint() < (rc.getType().paintCapacity + 1) / 2;

        // If robot has paint anchor, recharge to full
        if (paintTowerAnchor != null && paintLeqHalfFull) {
            int paintNeeded = rc.getType().paintCapacity - rc.getPaint();
            if (rc.canTransferPaint(paintTowerAnchor, -paintNeeded)) {
                rc.transferPaint(paintTowerAnchor, -paintNeeded);
            }
            else if (!rc.getLocation().isAdjacentTo(paintTowerAnchor)) {
                Direction dir = rc.getLocation().directionTo(paintTowerAnchor);
                if (rc.canMove(dir)) {
                    lastLocation = rc.getLocation();
                    rc.move(dir);
                }
            }
        }

        // If robot sees paint tower, set it to be paint anchor
        for (RobotInfo entity : nearbyEntities) {
            if (entity.getType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                paintTowerAnchor = entity.getLocation();
            }
        }

        // Search for a nearby ruin to complete.
        ArrayList<MapInfo> sensedRuins = new ArrayList<MapInfo>();
        for (MapInfo tile : nearbyTiles){
            if (tile.hasRuin()){
                sensedRuins.add(tile);
            }
        }
        for (MapInfo curRuin : sensedRuins) {
            boolean ruinHasTower = false;
            if (curRuin != null) {
                for (RobotInfo entity : nearbyEntities) {
                    if (entity.getLocation().equals(curRuin.getMapLocation())) {
                        ruinHasTower = true;
                    }
                }
            }
            if (curRuin != null && !ruinHasTower){
                MapLocation targetLoc = curRuin.getMapLocation();
                Direction dir = rc.getLocation().directionTo(targetLoc);
                // if (rc.canMove(dir)) {
                //     lastLocation = rc.getLocation();
                //     rc.move(dir);
                // }
                // Mark the pattern we need to draw to build a tower here if we haven't already. Pick a random tower of {paint, money} to build.
                MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
                UnitType towerToMark = (rng.nextInt(2) == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
                if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToMark, targetLoc)){
                    rc.markTowerPattern(towerToMark, targetLoc);
                }
                // Fill in any spots in the pattern with the appropriate paint.
                for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, rc.getType().actionRadiusSquared)){
                    if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                        boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(patternTile.getMapLocation()))
                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
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
    
            if (curRuin != null && ruinHasTower) {
                MapLocation ruinLoc = curRuin.getMapLocation();
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
        }
        
        List<Direction> dirs = Arrays.asList(Direction.allDirections());
        Collections.shuffle(dirs, rng);

        // try to paint resource pattern packing pattern
        for (Direction dir : dirs) {
            MapLocation loc = rc.getLocation().add(dir);
            boolean useSecondaryColor = false;
            if ((loc.x + loc.y) % 2 == 0 && (loc.x + loc.y * 3) % 10 != 0) {
                useSecondaryColor = true;
            }
            PaintType colorToPaint = (useSecondaryColor ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY);
            if (!rc.canSenseLocation(loc)) continue;
            MapInfo tile = rc.senseMapInfo(loc);
            if (tile.getMark().isAlly()) continue;
            if (tile.getPaint() != colorToPaint && rc.canPaint(loc) && rc.canAttack(loc)) {
                rc.attack(loc, useSecondaryColor);
            }
        }

        for (Direction dir : dirs) {
            MapLocation loc = rc.getLocation().add(dir);
            if ((loc.x + 3 * loc.y) % 10 == 0 && rc.canCompleteResourcePattern(loc)) {
                rc.setIndicatorDot(loc, 0, 0, 255);
                rc.completeResourcePattern(loc);
            }
        }
        
        boolean moved = false;
        for (Direction dir : dirs) {
            MapLocation nextLoc = rc.getLocation().add(dir);
            if (lastLocation != null && lastLocation != rc.getLocation() && nextLoc.isAdjacentTo(lastLocation))
                continue;
            if (rc.canMove(dir)) {
                lastLocation = rc.getLocation();
                rc.move(dir);
                moved = true;
            }
        }
        if (!moved) {
            for (Direction dir : dirs) {
                if (rc.canMove(dir)) {
                    lastLocation = rc.getLocation();
                    rc.move(dir);
                    moved = true;
                }
            }
        }

        rc.setIndicatorString("Paint tower anchor: " + paintTowerAnchor + ", Last location: " + lastLocation);
    }
    
}
