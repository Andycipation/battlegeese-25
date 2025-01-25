package prod_v19;

import battlecode.common.*;

public class Tower extends Robot {

    public static SpawnStrategy spawnStrat;
    public static int unitsBuilt = 0;

    public static CommsStrategy commsStrat = new CommsStrategyV2();
    public static final int TOWER_LETTER_LIMIT = GameConstants.MAX_MESSAGES_SENT_TOWER;
    public static int towerNumAtSpawn; // at the time of building, how many towers are there
    public static int roundNumAtSpawn;

    public static MapLocation informedEnemyPaintLoc = null;
    public static int informedEnemyPaintLocTimestamp = -1;
    public static MapLocation informedEmptyPaintLoc = null;
    public static int informedEmptyPaintLocTimestamp = -1;

    public Tower() {
        super();
        towerNumAtSpawn = numTowers;
        roundNumAtSpawn = rc.getRoundNum();
        spawnStrat = new MapSpawnStrategy();
    }
    
    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        commsStrat.receiveAndBroadcast();
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
        playSpawnUnits();
        playAttack();
    }

    static abstract class CommsStrategy extends Tower {
        abstract public void receiveAndBroadcast() throws GameActionException;
    }

    static class CommsStrategyV2 extends CommsStrategy {

        public static MapLocation emptyLoc = null;
        public static int emptyLocTimestamp = -1;
        public static MapLocation enemyLoc = null;
        public static int enemyLocTimestamp = -1;

        public static boolean chkEmptyLoc(MapLocation loc, int timestamp) {
            if (timestamp > emptyLocTimestamp) {
                emptyLoc = loc;
                emptyLocTimestamp = timestamp;
                return true;
            }
            return false;
        }
        public static boolean chkEnemyLoc(MapLocation loc, int timestamp) {
            if (timestamp > enemyLocTimestamp) {
                enemyLoc = loc;
                enemyLocTimestamp = timestamp;
                return true;
            }
            return false;
        }

        @Override
        public void receiveAndBroadcast() throws GameActionException {
            
            // process informs from last round
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

            int offset = rng.nextInt(nearbyMapInfos.length);

            for (int i = nearbyMapInfos.length; --i >= 0; ) {
                MapInfo tile = nearbyMapInfos[(i + offset) % nearbyMapInfos.length];
                MapLocation loc = tile.getMapLocation();
                if (!tile.isPassable()) continue;
                if (tile.getPaint() == PaintType.EMPTY) {
                    chkEmptyLoc(loc, roundNum);
                }
                if (tile.getPaint().isEnemy()) {
                    chkEnemyLoc(loc, roundNum);
                }
            }
            
            for (int i = lastRoundMessages.length; --i >= 0; ) {
                int bytes = lastRoundMessages[i].getBytes();
                if (Comms.getProtocol(bytes) == Comms.Protocol.TOWER_TO_TOWER_V2) {
                    int[] data = Comms.towerToTowerCommsV2.decode(bytes);
                    int network = data[1];
                    int timestamp = data[2];
                    MapLocation loc = Comms.decodeMapLocation(data[3]);
                    if (network == 0) chkEmptyLoc(loc, timestamp);
                    else chkEnemyLoc(loc, timestamp);
                }
            }
            if (emptyLoc != null) {
                Logger.log("next empty: " + emptyLoc);
                rc.setIndicatorLine(locBeforeTurn, emptyLoc, 0, 0, 255);
            }
            if (enemyLoc != null) {
                Logger.log("next enemy: " + enemyLoc);
                rc.setIndicatorLine(locBeforeTurn, enemyLoc, 255, 0, 0);
            }
    
            { // send message to turrets
                int network = roundNum % 2;
                MapLocation transmitLoc = (network == 0 ? emptyLoc : enemyLoc);
                int transmitTimestamp = (network == 0 ? emptyLocTimestamp : enemyLocTimestamp);
                if (transmitLoc != null) {
                    rc.broadcastMessage(Comms.towerToTowerCommsV2.encode(
                        new int[]{Comms.Protocol.TOWER_TO_TOWER_V2.ordinal(),
                                    network,
                                    transmitTimestamp,
                                    Comms.encodeMapLocation(transmitLoc)}));
                }
            }

            { // send message to units
                int numLetters = 0;
                int network = roundNum % 2;
                MapLocation transmitLoc = (network == 0 ? emptyLoc : enemyLoc);
                int transmitTimestamp = (network == 0 ? emptyLocTimestamp : enemyLocTimestamp);
                if (transmitLoc != null) {
                    for (int i = nearbyAllyRobots.length; --i >= 0;) {
                        RobotInfo robot = nearbyAllyRobots[i];
                        if (robot.getType().isTowerType()) continue;
                        MapLocation loc = robot.location;
                        int msg = Comms.towerNetworkInformComms.encode(new int[]{
                            Comms.Protocol.TOWER_NETWORK_INFORM.ordinal(),
                            network,
                            transmitTimestamp, // enemy network
                            Comms.encodeMapLocation(transmitLoc)
                        });
                        if (numLetters < TOWER_LETTER_LIMIT-1 && rc.canSendMessage(loc)) {
                            numLetters++;
                            rc.sendMessage(loc, msg);
                        }
                        if (numLetters == TOWER_LETTER_LIMIT-1) break;
                    }
                }
            }

            informedEnemyPaintLoc = enemyLoc;
            informedEnemyPaintLocTimestamp = enemyLocTimestamp;
            informedEmptyPaintLoc = emptyLoc;
            informedEmptyPaintLocTimestamp = emptyLocTimestamp;
        }

    }

    public static abstract class SpawnStrategy {
        abstract public void act() throws GameActionException;
    }

    static class MapSpawnStrategy extends SpawnStrategy {

        static int initialSoldiersToSpawn;
        static Direction dirToCenter;
        static Direction nextSpawnDir;
        static int numMoppersSpawned;

        public MapSpawnStrategy() {
            // Need to compute this in here, doesn't work to get the Globals variable
            final int towerNumAtSpawn = rc.getNumberTowers();
            // System.out.println("spawned with " + towerNumAtSpawn + " towers alive");

            // Compute the initial number of soldiers to spawn'
            if (towerNumAtSpawn <= 2) {
                if (isPaintTower(rc.getType())) {
                    initialSoldiersToSpawn = switch (mapSize) {
                        case SMALL -> 2;
                        case MEDIUM -> 3;
                        case LARGE -> 4;
                    };
                } else {
                    initialSoldiersToSpawn = 2;
                }
            } else {
                initialSoldiersToSpawn = 0;
            }
            numMoppersSpawned = 0;

            var mapCenter = new MapLocation(mapWidth / 2, mapHeight / 2);
            var myLoc = rc.getLocation();
            dirToCenter = myLoc.directionTo(mapCenter);
            nextSpawnDir = dirToCenter;
        }

        public static Direction tryBuildUnit(UnitType unitType, Direction preferredDir) throws GameActionException {
            // Returns the direction the unit was built, if successful, or null otherwise
            var dir = preferredDir;
            for (int i = 8; --i >= 0;) {
                var loc = locBeforeTurn.add(dir);
                if (loc.add(dir).distanceSquaredTo(locBeforeTurn) <= 4) {
                    loc = loc.add(dir);
                }
                if (rc.canBuildRobot(unitType, loc)) {
                    rc.buildRobot(unitType, loc);
                    unitsBuilt++;
                    return dir;
                }
                dir = dir.rotateRight();
            }
            return null;
        }

        public static void tryBuildRandomUnit(int soldierWeight, int splasherWeight, int mopperWeight) throws GameActionException {
            UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                case 0 -> UnitType.SOLDIER;
                case 1 -> UnitType.SPLASHER;
                default -> UnitType.MOPPER;
            };
            tryBuildUnit(unitPicked, dirToCenter);
        }

        static boolean hasAdjacentAlly() {
            for (int i = nearbyAllyRobots.length; --i >= 0;) {
                var info = nearbyAllyRobots[i];
                if (info.location.distanceSquaredTo(rc.getLocation()) <= 2) {
                    return true;
                }
            }
            return false;
        }

        static MapLocation hasNearbyEnemy(UnitType type1, UnitType type2) {
            for (int i = nearbyEnemyRobots.length; --i >= 0;) {
                var info = nearbyEnemyRobots[i];
                if (info.type == type1 || info.type == type2) {
                    return info.location;
                }
            }
            return null;
        }

        @Override
        public void act() throws GameActionException {
            if (roundNum > 20 && roundNum < roundNumAtSpawn + 2 && hasAdjacentAlly()) {
                // Don't spawn right away early game in case someone is waiting to withdraw paint
                return;
            }
            if (initialSoldiersToSpawn > 0) {
                if (tryBuildUnit(UnitType.SOLDIER, nextSpawnDir) != null) {
                    initialSoldiersToSpawn -= 1;
                    nextSpawnDir = nextSpawnDir.rotateRight();
                }
                return;
            }

            if (numMoppersSpawned < 3) {
                var enemyLoc = hasNearbyEnemy(UnitType.SOLDIER, UnitType.SPLASHER);
                if (enemyLoc != null) {
                    var dir = rc.getLocation().directionTo(enemyLoc);
                    if (tryBuildUnit(UnitType.MOPPER, dir) != null) {
                        numMoppersSpawned += 1;
                        return;
                    }
                }
            }

            if (isPaintTower(rc.getType())) {
                if (rc.getChips() > 1400 && rc.getPaint() >= 300) {
                    // Pick random unit to build (with weights)
                    if (informedEnemyPaintLoc != null) {
                        tryBuildRandomUnit(1, 1, 1);
                    } else if (rng.nextInt(3) == 0) {
                        tryBuildUnit(UnitType.SOLDIER, dirToCenter);
                    }
                }
            } else {
                // defense or money towers can always just produce units given theres enough chips :)
                if (rc.getChips() > 1250) {
                    tryBuildUnit(UnitType.SOLDIER, dirToCenter);
                    // tryBuildUnit(UnitType.MOPPER, dirToCenter);
                }
            }
        }

    }
}