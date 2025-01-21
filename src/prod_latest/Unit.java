package prod_latest;

import battlecode.common.*;

public abstract class Unit extends Robot {

    static public MapLocation paintTowerLoc;

    static public MapLocation informedEnemyPaintLoc = null;
    static public int informedEnemyPaintLocTimestamp = -1;
    static public MapLocation informedEmptyPaintLoc = null;
    static public int informedEmptyPaintLocTimestamp = -1;

    public static enum MapLocationType {
        PASSABLE, WALL, RUIN
    };
    public static MapLocationType[][] memory;

    // public Unit() {
    //     memory = new MapLocationType[mapWidth][mapHeight];
    // }
    
    public static boolean chkEmptyLoc(MapLocation loc, int timestamp) {
        if (timestamp > informedEmptyPaintLocTimestamp) {
            informedEmptyPaintLoc = loc;
            informedEmptyPaintLocTimestamp = timestamp;
            return true;
        }
        return false;
    }
    public static boolean chkEnemyLoc(MapLocation loc, int timestamp) {
        if (timestamp > informedEnemyPaintLocTimestamp) {
            informedEnemyPaintLoc = loc;
            informedEnemyPaintLocTimestamp = timestamp;
            return true;
        }
        return false;
    }

    /**
     * Perform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {
        // if (memory == null) {
        //     memory = new MapLocationType[mapWidth][mapHeight];
        // }
        super.initTurn();
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            var info = nearbyAllyRobots[i];
            if (Globals.isAllyPaintTower(info)) {
                paintTowerLoc = info.location;
            }
        }

        // for (int i = nearbyMapInfos.length; --i >= 0;) {
        //     MapInfo tile = nearbyMapInfos[i];
        //     MapLocation loc = tile.getMapLocation();
        //     memory[loc.x][loc.y] = tile.isPassable() ? MapLocationType.PASSABLE : (tile.isWall() ? MapLocationType.WALL : MapLocationType.RUIN);
        // }

        for (int i = nearbyMapInfos.length; --i >= 0;) {
            MapInfo tile = nearbyMapInfos[i];
            if (!tile.isPassable()) continue;
            MapLocation loc = tile.getMapLocation();
            if (tile.getPaint().isEnemy()) {
                informedEnemyPaintLoc = loc;
                informedEnemyPaintLocTimestamp = roundNum;
            } else if (tile.getPaint() == PaintType.EMPTY) {
                informedEmptyPaintLoc = loc;
                informedEmptyPaintLocTimestamp = roundNum;
            }
        }

        for (int i = lastRoundMessages.length; --i >= 0;) {
            int messageBytes = lastRoundMessages[i].getBytes();
            switch (Comms.getProtocol(messageBytes)) {
                case TOWER_NETWORK_INFORM: {
                    int[] decoded = Comms.towerNetworkInformComms.decode(messageBytes);
                    boolean enemyNetwork = decoded[1] == 1;
                    int timestamp = decoded[2];
                    MapLocation loc = Comms.decodeMapLocation(decoded[3]);
                    if (enemyNetwork) chkEnemyLoc(loc, timestamp);
                    else chkEmptyLoc(loc, timestamp);
                    break;
                }
                default:
                    break;
            }
        }

    }

    /**
     * Perform main robot turn actions. This includes moving and attacking.
     */
    void play() throws GameActionException {
        super.play();
    }

    /**
     * Perform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {
        super.endTurn();

        int networkToSend = -1;
        if (informedEnemyPaintLoc == null && informedEmptyPaintLoc == null) {}
        else if (informedEnemyPaintLoc != null && informedEmptyPaintLoc == null) {
            networkToSend = 1;
        } else if (informedEnemyPaintLoc == null && informedEmptyPaintLoc != null) {
            networkToSend = 0;
        } else {
            networkToSend = rng.nextInt(2);
        }
        MapLocation msgRecipient = null;
        for (int i = nearbyAllyRobots.length; --i >= 0;) {
            RobotInfo robotInfo = nearbyAllyRobots[i];
            MapLocation loc = robotInfo.location;
            if (robotInfo.getType().isTowerType() && rc.canSendMessage(loc)) {
                msgRecipient = robotInfo.location;
                break;
            }
        }
        if (networkToSend != -1 && msgRecipient != null) {
            MapLocation informLoc = (networkToSend == 1 ? informedEnemyPaintLoc : informedEmptyPaintLoc);
            int timestamp = (networkToSend == 1 ? informedEnemyPaintLocTimestamp : informedEmptyPaintLocTimestamp);
            int msg = Comms.towerNetworkInformComms.encode(new int[]{
                Comms.Protocal.TOWER_NETWORK_INFORM.ordinal(),
                networkToSend,
                timestamp,
                Comms.encodeMapLocation(informLoc)
            });
            rc.sendMessage(msgRecipient, msg);
        }

        if (informedEnemyPaintLoc != null) {
            rc.setIndicatorLine(rc.getLocation(), informedEnemyPaintLoc, 255, 150, 150);
        }
        if (informedEmptyPaintLoc != null) {
            // rc.setIndicatorLine(locBeforeTurn, informedEmptyPaintLoc, 150, 150, 255);
        }
    }

    void upgradeTowers() throws GameActionException {
        for (int i = nearbyAllyRobots.length; --i >= 0;)  {
            RobotInfo robotInfo = nearbyAllyRobots[i];
            if (robotInfo.type.canUpgradeType() && rc.getChips() > 10000) {
                if (rc.canUpgradeTower(robotInfo.location)) {
                    rc.upgradeTower(robotInfo.location);
                }
            }
        }
    }


    public static boolean tryMoveToFrontier() throws GameActionException {
        MapLocation attackLoc = informedEnemyPaintLoc;

        if (attackLoc != null && rc.getLocation().isWithinDistanceSquared(attackLoc, visionRadiusSquared) 
        && rc.senseMapInfo(attackLoc).getPaint() == PaintType.EMPTY) {
            attackLoc = null;
        }

        // Optional optimization, remove if degrades performance or uses excess bytecode
        for (int i = adjacentDirections.length; --i >= 0;) {
            Direction dir = adjacentDirections[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (tile.getPaint().isEnemy()) {
                attackLoc = nxtLoc;
                break;
            }
        }

        if (attackLoc != null) {
            Direction dir = BugNav.getDirectionToMove(attackLoc);
            if (dir != null && rc.canMove(dir)) {
                MapLocation nxtLoc = rc.getLocation().add(dir);
                MapInfo tile = rc.senseMapInfo(nxtLoc);
                if (!tile.getPaint().isEnemy() && !inEnemyTowerRange(nxtLoc)) {
                    rc.move(dir);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean inEnemyTowerRange(MapLocation loc) throws GameActionException {
        for (int i = nearbyEnemyRobots.length; --i >= 0;) {
            RobotInfo robotInfo = nearbyEnemyRobots[i];
            MapLocation enemyLoc = nearbyEnemyRobots[i].getLocation();
            if (robotInfo.getType().isTowerType() && loc.isWithinDistanceSquared(enemyLoc, robotInfo.getType().actionRadiusSquared)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMoveToSafeTile() throws GameActionException {
        Direction[] dirs = adjacentDirections.clone();
        shuffleArray(dirs);
        Direction bestDir = null;
        int minPenalty = 1000;
        for (int i = dirs.length; --i >= 0;) {
            Direction dir = dirs[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (tile.getPaint().isAlly() && rc.canMove(dir) && !inEnemyTowerRange(nxtLoc)) {
                int penalty = numAllyAdjacent[dir.getDirectionOrderNum()];
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
            return true;
        }
        return false;
    }

    public static boolean tryMoveLessSafeTile() throws GameActionException {
        Direction[] dirs = adjacentDirections.clone();
        shuffleArray(dirs);
        Direction bestDir = null;
        int minPenalty = 100;
        for (int i = dirs.length; --i >= 0;) {
            Direction dir = dirs[i];
            MapLocation nxtLoc = rc.getLocation().add(dir);
            if (!withinBounds(nxtLoc)) continue;
            MapInfo tile = rc.senseMapInfo(nxtLoc);
            if (!tile.getPaint().isEnemy() && rc.canMove(dir) && !inEnemyTowerRange(nxtLoc)) {
                int penalty = numAllyAdjacent[dir.getDirectionOrderNum()];
                if (penalty < minPenalty) {
                    minPenalty = penalty;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
            return true;
        }
        return false;
    }
}
