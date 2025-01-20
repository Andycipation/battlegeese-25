package prod_jan_19_9_pm;

import battlecode.common.*;

public class Tower extends Robot {

    public static SpawnStrategy spawnStrat;
    public static int unitsBuilt = 0;
    public static CircularBuffer<MapLocation> sensedEmptyLocs = new CircularBuffer<MapLocation>(3);
    public static CircularBuffer<MapLocation> sensedEnemyLocs = new CircularBuffer<MapLocation>(3);
    public static Comms towerToTowerComms = new Comms(new int[]{Comms.IDENTIFIER_SZ, GameConstants.MAX_NUMBER_OF_TOWERS, GameConstants.MAX_NUMBER_OF_TOWERS, Comms.MAP_ENCODE_SZ});
    public static int emptyLevel = GameConstants.MAX_NUMBER_OF_TOWERS - 1;
    public static int enemyLevel = GameConstants.MAX_NUMBER_OF_TOWERS - 1;
    
    @Override
    void initTurn() throws GameActionException {
        super.initTurn();

        if (roundNum % 10 == 0) { // reset sensed enemy/empty after some period of time
            sensedEmptyLocs.clear();
            sensedEnemyLocs.clear();
            emptyLevel = GameConstants.MAX_NUMBER_OF_TOWERS - 1;
            enemyLevel = GameConstants.MAX_NUMBER_OF_TOWERS - 1;
        }

        for (int i = nearbyMapInfos.length; --i >= 0; ) {
            MapInfo tile = nearbyMapInfos[i];
            MapLocation loc = tile.getMapLocation();
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
            if (Comms.getProtocol(bytes) == Comms.Protocal.TOWER_TO_TOWER) {
                int[] data = towerToTowerComms.decode(bytes);
                int relaxEmptyLevel = Math.min(data[1] + 1, GameConstants.MAX_NUMBER_OF_TOWERS - 1);
                int relaxEnemyLevel = Math.min(data[2] + 1, GameConstants.MAX_NUMBER_OF_TOWERS - 1);
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

        rc.broadcastMessage(towerToTowerComms.encode(
            new int[]{Comms.Protocal.TOWER_TO_TOWER.ordinal(),
                      emptyLevel,
                      enemyLevel,
                      Comms.encodeMapLocation(locBeforeTurn)}));
        
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
        // System.out.println("progress: " + getProgress());

        if (spawnStrat == null) {
            switch (mapCategory) {
                case SIZE1 -> spawnStrat = new Size1MapSpawnStrategy();
                case SIZE2 -> spawnStrat = new Size2MapSpawnStrategy();
                case SIZE3 -> spawnStrat = new Size3MapSpawnStrategy();
            }
        }

        playSpawnUnits();
        playAttack();

    }

    static abstract class SpawnStrategy extends Tower {
        abstract public void act() throws GameActionException;
    }

    static class Size1MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 15;
                int splasherWeight = 3 * numTowers;
                int mopperWeight = 2 * numTowers;
                UnitType unitPicked = switch (randChoice(soldierWeight, splasherWeight, mopperWeight)) {
                    case 0 -> UnitType.SOLDIER;
                    case 1 -> UnitType.SPLASHER;
                    default -> UnitType.MOPPER;
                };
                tryBuildUnit(unitPicked);
            }
        }
    }

    static class Size2MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
            }
            else if (rc.getChips() > 1300 && rc.getPaint() >= 300) { // can build any unit and still have chips left to make a tower
                // pick random unit to build (with weights)
                int soldierWeight = 16;
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

    static class Size3MapSpawnStrategy extends SpawnStrategy {
        public void act() throws GameActionException {
            if (unitsBuilt < 3) { // early game build soldier soldier mopper
                if (unitsBuilt < 2) tryBuildUnit(UnitType.SOLDIER);
                else tryBuildUnit(UnitType.MOPPER);
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
