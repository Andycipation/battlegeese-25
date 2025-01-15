package template;

import battlecode.common.*;

public abstract class Robot extends Globals {

    /**
     * Preform actions at the beginning of the robot's turn.
     * Here we can record new information, update robot state, etc...
     */
    void initTurn() throws GameActionException {

    }

    /**
     * Preform main robot turn actions. This includes moving and attacking.
     */
    abstract void play() throws GameActionException;

    /**
     * Preform actions at the end of robot's turn. This can be used for cleanup
     * and/or for using spare bytecode to preform expensive calculations that may
     * span across several turns.
     */
    void endTurn() throws GameActionException {

    }
    
}
