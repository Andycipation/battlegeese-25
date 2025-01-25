package prod_v14;

import battlecode.common.*;

public class BugNav extends Globals {
    private static MapLocation currentTarget;

    private static int minDistanceToTarget;
    private static boolean obstacleOnRight;
    private static MapLocation currentObstacle;
    private static FastSet visitedStates;

    private static boolean isStuck() {
        for (int i = adjacentDirections.length; --i >= 0; ) {
            if (rc.canMove(adjacentDirections[i])) {
                return false;
            }
        }
        return true;
    }

    private static Direction followWall(boolean canRotate) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(currentObstacle);

        for (int i = 8; --i >= 0; ) {
            dir = obstacleOnRight ? dir.rotateLeft() : dir.rotateRight();
            if (rc.canMove(dir)) {
                return dir;
            }

            MapLocation location = rc.adjacentLocation(dir);
            if (canRotate && !rc.onTheMap(location)) {
                obstacleOnRight = !obstacleOnRight;
                return followWall(false);
            }

            if (rc.onTheMap(location) && !rc.sensePassability(location)) {
                currentObstacle = location;
            }
        }

        // Should never get here, because we checked that the robot is not stuck
        // assert(false);
        return null;
    }

    public static Direction getDirectionToMove(MapLocation target) throws GameActionException {
        // Returns the direction to move to get closer to `target`. The returned direction is
        // guaranteed to be passable, i.e. rc.move(dir) will never throw.
        // Returns null if there is no adjacent direction that is passable.
        if (currentTarget == null || !currentTarget.equals(target)) {
            reset();
        }

        if (isStuck()) {
            return null;
        }

        MapLocation myLocation = rc.getLocation();

        int distanceToTarget = chebyshevDist(myLocation, target);
        if (distanceToTarget < minDistanceToTarget) {
            reset();
            minDistanceToTarget = distanceToTarget;
        }

        if (currentObstacle != null && rc.canSenseLocation(currentObstacle) && rc.sensePassability(currentObstacle)) {
            reset();
        }

        if (!visitedStates.add(getState(target))) {
            reset();
        }

        currentTarget = target;

        if (currentObstacle == null) {
            Direction forward = myLocation.directionTo(target);
            if (rc.canMove(forward)) {
                return forward;
            }
            setInitialDirection();
        }

        return followWall(true);
    }

    public static boolean moveToward(MapLocation target) throws GameActionException {
        var dir = getDirectionToMove(target);
        if (dir != null) {
            assert(rc.canMove(dir));
            rc.move(dir);
            return true;
        }
        return false;
    }

    public static void reset() {
        currentTarget = null;
        minDistanceToTarget = Integer.MAX_VALUE;
        obstacleOnRight = true;
        currentObstacle = null;
        visitedStates = new FastSet();
    }

    private static void setInitialDirection() throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        Direction forward = myLocation.directionTo(currentTarget);

        Direction left = forward.rotateLeft();
        for (int i = 8; --i >= 0; ) {
            MapLocation location = rc.adjacentLocation(left);
            if (rc.onTheMap(location) && rc.sensePassability(location)) {
                break;
            }
            left = left.rotateLeft();
        }

        Direction right = forward.rotateRight();
        for (int i = 8; --i >= 0; ) {
            MapLocation location = rc.adjacentLocation(right);
            if (rc.onTheMap(location) && rc.sensePassability(location)) {
                break;
            }
            right = right.rotateRight();
        }

        MapLocation leftLocation = rc.adjacentLocation(left);
        MapLocation rightLocation = rc.adjacentLocation(right);

        int leftDistance = chebyshevDist(leftLocation, currentTarget);
        int rightDistance = chebyshevDist(rightLocation, currentTarget);

        if (leftDistance < rightDistance) {
            obstacleOnRight = true;
        } else if (rightDistance < leftDistance) {
            obstacleOnRight = false;
        } else {
            obstacleOnRight = myLocation.distanceSquaredTo(leftLocation) < myLocation.distanceSquaredTo(rightLocation);
        }

        if (obstacleOnRight) {
            currentObstacle = rc.adjacentLocation(left.rotateRight());
        } else {
            currentObstacle = rc.adjacentLocation(right.rotateLeft());
        }
    }

    private static char getState(MapLocation target) {
        MapLocation myLocation = rc.getLocation();
        Direction direction = myLocation.directionTo(currentObstacle != null ? currentObstacle : target);
        int rotation = obstacleOnRight ? 1 : 0;

        return (char) ((((myLocation.x << 6) | myLocation.y) << 4) | (direction.ordinal() << 1) | rotation);
    }
}