package prod_v7;

import battlecode.common.*;

public class Tower extends Robot {

    public static SpawnStrategy spawnStrat;
    public static int unitsBuilt = 0;

    public static CommsStrategy commsStrat = new CommsStrategyV2();
    public static final int TOWER_LETTER_LIMIT = GameConstants.MAX_MESSAGES_SENT_TOWER;
    public static int towerNumAtSpawn; // at the time of building, how many towers are there

    public Tower() {
        super();
        towerNumAtSpawn = numTowers;
    }
    
    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        commsStrat.receiveAndBroadcast();
        // Letter[] letters = commsStrat.prepareLetters();
        // dispatchLetters(letters);
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

        // if (spawnStrat == null) {
        //     switch (mapCategory) {
        //         case SIZE1 -> spawnStrat = new Size1MapSpawnStrategy();
        //         case SIZE2 -> spawnStrat = new Size2MapSpawnStrategy();
        //         case SIZE3 -> spawnStrat = new Size3MapSpawnStrategy();
        //     }
        // }
        spawnStrat = new MapSpawnStrategy();

        playSpawnUnits();
        playAttack();

    }

    static abstract class CommsStrategy extends Tower {
        abstract public void receiveAndBroadcast() throws GameActionException;
    }

    static class CommsStrategyV1 extends CommsStrategy {

        public static CircularBuffer<MapLocation> sensedEmptyLocs = new CircularBuffer<MapLocation>(3);
        public static CircularBuffer<MapLocation> sensedEnemyLocs = new CircularBuffer<MapLocation>(3);
        public static int UNDETECTED_LEVEL = 10;
        public static int emptyLevel = UNDETECTED_LEVEL;
        public static int enemyLevel = UNDETECTED_LEVEL;
        private static class Letter {
            int messageBytes, toId;
            public Letter(int _messageBytes, int _toId) {
                messageBytes = _messageBytes;
                toId = _toId;
            }
        };

        @Override
        public void receiveAndBroadcast() throws GameActionException {
            if (roundNum % 10 == 0) { // reset sensed enemy/empty after some period of time
                // sensedEmptyLocs.clear();
                // sensedEnemyLocs.clear();
                emptyLevel++; // raise my level a bit
                enemyLevel++; // raise my level a bit
            }
    
            for (int i = nearbyMapInfos.length; --i >= 0; ) {
                MapInfo tile = nearbyMapInfos[i];
                MapLocation loc = tile.getMapLocation();
                if (!tile.isPassable()) continue;
                if (tile.getPaint() == PaintType.EMPTY) {
                    sensedEmptyLocs.push(loc);
                    emptyLevel = 0;
                }
                if (tile.getPaint().isEnemy()) {
                    sensedEnemyLocs.push(loc);
                    enemyLevel = 0;
                }
            }
            
            for (int i = lastRoundMessages.length; --i >= 0; ) {
                int bytes = lastRoundMessages[i].getBytes();
                if (Comms.getProtocol(bytes) == Comms.Protocal.TOWER_TO_TOWER_V1) {
                    int[] data = Comms.towerToTowerCommsV1.decode(bytes);
                    int relaxEmptyLevel = Math.min(data[1] + 1, UNDETECTED_LEVEL);
                    int relaxEnemyLevel = Math.min(data[2] + 1, UNDETECTED_LEVEL);
                    MapLocation adjLoc = Comms.decodeMapLocation(data[3]);
                    if (relaxEmptyLevel < emptyLevel) {
                        sensedEmptyLocs.clear();
                        emptyLevel = relaxEmptyLevel;
                    }
                    if (relaxEnemyLevel < enemyLevel) {
                        sensedEnemyLocs.clear();
                        enemyLevel = relaxEnemyLevel;
                    }
                    if (relaxEmptyLevel == emptyLevel) sensedEmptyLocs.push(adjLoc);
                    if (relaxEnemyLevel == enemyLevel) sensedEnemyLocs.push(adjLoc);
                }
            }
            if (!sensedEmptyLocs.empty()) {
                Logger.log("next empty: " + sensedEmptyLocs.poll());
                // rc.setIndicatorLine(locBeforeTurn, sensedEmptyLocs.poll(), 0, 255, 255);
            }
            if (!sensedEnemyLocs.empty()) {
                Logger.log("next enemy: " + sensedEnemyLocs.poll());
                rc.setIndicatorLine(locBeforeTurn, sensedEnemyLocs.poll(), 0, 255 - 30 * enemyLevel, 255 - 30 * enemyLevel);
            }
    
            rc.broadcastMessage(Comms.towerToTowerCommsV1.encode(
                new int[]{Comms.Protocal.TOWER_TO_TOWER_V1.ordinal(),
                          emptyLevel,
                          enemyLevel,
                          Comms.encodeMapLocation(locBeforeTurn)}));

            dispatchLetters(prepareLetters());
        }

        public Letter[] prepareLetters() throws GameActionException {
            int numLetters = 0;
            Letter[] lettersBuf = new Letter[TOWER_LETTER_LIMIT];
    
            // process requests from last round
            for (int i = lastRoundMessages.length; --i >= 0;) {
                int messageBytes = lastRoundMessages[i].getBytes();
                switch (Comms.getProtocol(messageBytes)) {
                    case TOWER_NETWORK_REQUEST:
                        int[] decoded = Comms.towerNetworkRequestComms.decode(messageBytes);
                        boolean moveForward = decoded[1] == 1;
                        boolean enemyNetwork = decoded[2] == 1;
                        int requestorId = decoded[3];
                        CircularBuffer<MapLocation> buffer = (enemyNetwork ? sensedEnemyLocs : sensedEmptyLocs);
                        int requestedLevel = (enemyNetwork ? enemyLevel : emptyLevel);
                        if (moveForward) {
                            int msg;
                            if (requestedLevel == UNDETECTED_LEVEL || buffer.empty()) {
                                msg = Comms.towerNetworkResponseComms.encode(new int[]{
                                    Comms.Protocal.TOWER_NETWORK_RESPONSE.ordinal(),
                                    0, // not successful
                                    0 // random value
                                });
                            }
                            else {
                                msg = Comms.towerNetworkResponseComms.encode(new int[]{
                                    Comms.Protocal.TOWER_NETWORK_RESPONSE.ordinal(),
                                    1, // successful
                                    Comms.encodeMapLocation(buffer.poll()) // next location in network
                                });
                            }
                            if (numLetters < TOWER_LETTER_LIMIT)
                                lettersBuf[numLetters++] = new Letter(msg, requestorId);
                        }
                        else {
                            // NOT IMPLEMENTED YET...
                        }
                    default:
                        break;
                }
            }

            return lettersBuf;
        }

        public void dispatchLetters(Letter[] letters) throws GameActionException {
            int numLetters = letters.length;
    
            // dispatch letters to send
            FastMap<MapLocation> idToLoc = new FastMap<MapLocation>(100);
            for (int i = nearbyAllyRobots.length; --i >= 0;) {
                RobotInfo robot = nearbyAllyRobots[i];
                idToLoc.add((char)robot.getID(), robot.getLocation());
            }
    
            for (int i = numLetters; --i >= 0;) {
                Letter letter = letters[i];
                if (letter == null) continue;
                int messageBytes = letter.messageBytes;
                int toId = letter.toId;
                MapLocation loc = idToLoc.get((char)toId);
                if (loc != null && rc.canSendMessage(loc, messageBytes)) {
                    rc.sendMessage(loc, messageBytes);
                }
            }
        }
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
                if (Comms.getProtocol(bytes) == Comms.Protocal.TOWER_TO_TOWER_V2) {
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
                        new int[]{Comms.Protocal.TOWER_TO_TOWER_V2.ordinal(),
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
                            Comms.Protocal.TOWER_NETWORK_INFORM.ordinal(),
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

    static abstract class SpawnStrategy extends Tower {
        abstract public void act() throws GameActionException;
    }

    static class MapSpawnStrategy extends SpawnStrategy {

        @Override
        public void act() throws GameActionException {
            if (unitsBuilt < 2 && towerNumAtSpawn <= 2) { // early game build soldier soldier
                tryBuildUnit(UnitType.SOLDIER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 20;
                int splasherWeight = numTowers;
                int mopperWeight = numTowers;
                UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                    case 0 -> UnitType.SOLDIER;
                    case 1 -> UnitType.SPLASHER;
                    default -> UnitType.MOPPER;
                };
                tryBuildUnit(unitPicked);
            }
        }

    }
}