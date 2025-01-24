package prod_latest;

import battlecode.common.*;

public class Profiler extends Globals {

    int startRound;
    int startBytecodes;
    static int bytecodeLimit;


    Profiler() {
        startRound = rc.getRoundNum();
        startBytecodes = Clock.getBytecodeNum();
        bytecodeLimit = unitType.isTowerType() ? GameConstants.TOWER_BYTECODE_LIMIT : GameConstants.ROBOT_BYTECODE_LIMIT;

    }
    void printBytecodeUsed(String s) {
        int endRound = rc.getRoundNum();
        int endBytecodes = Clock.getBytecodeNum();
        int usedBytecodes = startRound == endRound
            ? endBytecodes - startBytecodes
            : (bytecodeLimit - startBytecodes) + Math.max(0, endRound - startRound - 1) * bytecodeLimit + endBytecodes;
        System.out.println(rc.getLocation() + " " + rc.getType() + " " + s + " Bytecode used: " + usedBytecodes);
    }
}
