package prod_v9;

import battlecode.common.*;

public class Tower extends Robot {

    public static SpawnStrategy spawnStrat;
    public static int unitsBuilt = 0;

    public static CommsStrategy commsStrat = new CommsStrategyV2();
    public static final int TOWER_LETTER_LIMIT = GameConstants.MAX_MESSAGES_SENT_TOWER;
    public static int towerNumAtSpawn; // at the time of building, how many towers are there
    public static int roundNumAtSpawn;

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

    public static boolean tryBuildUnit(UnitType robotType) throws GameActionException {
        for (int i = adjacentDirections.length; --i >= 0; ) {
            MapLocation loc = locBeforeTurn.add(adjacentDirections[i]);
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
    
            for (int i = nearbyMapInfos.length; --i >= 0; ) {
                MapInfo tile = nearbyMapInfos[i];
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
        }

    }

    public static abstract class SpawnStrategy {
        abstract public void act() throws GameActionException;
    }

    static class MapSpawnStrategy extends SpawnStrategy {

        public static void tryBuildRandomUnit(int soldierWeight, int splasherWeight, int mopperWeight) throws GameActionException {
            UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                case 0 -> UnitType.SOLDIER;
                case 1 -> UnitType.SPLASHER;
                default -> UnitType.MOPPER;
            };
            tryBuildUnit(unitPicked);
        }

        @Override
        public void act() throws GameActionException {
            if (rc.getRoundNum() < 20 && rc.getRoundNum() < roundNumAtSpawn + 2) {
                // Don't spawn right away early game in case someone is waiting to withdraw paint
                return;
            }
            if (unitsBuilt < 2 && towerNumAtSpawn <= 2) {
                // early game build soldier soldier
                tryBuildUnit(UnitType.SOLDIER);
                return;
            }
            if (isPaintTower(rc.getType())) {
                if ((rc.getRoundNum() > 300 && rc.getPaint() >= 800) || (rc.getChips() > 1400 && rc.getPaint() >= 300)) {
                    // Pick random unit to build (with weights)
                    tryBuildRandomUnit(20, numTowers, numTowers);
                }
            } 
            else { // defense or money towers can always just produce units given theres enough chips :)
                if (rc.getChips() > 1400) {
                    tryBuildRandomUnit(20, numTowers, numTowers);
                }
            }
        }

    }
}