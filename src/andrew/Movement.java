package andrew;

import battlecode.common.*;

public class Movement extends Globals {
    private static MapLocation currentTarget;
    private static int minDistanceToTarget;
    private static boolean obstacleOnRight;
    private static MapLocation currentObstacle;
    private static FastSet visitedStates;

    public static boolean tryMove(Direction direction) throws GameActionException {
        assert(direction != null);
        if (rc.canMove(direction)) {
            rc.move(direction);
            return true;
        }
        return false;
    }

    public static Direction getMove(MapLocation target) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        assert(myLocation != null && target != null);
        if (currentTarget == null || !currentTarget.equals(target)) {
            reset();
        }

        boolean hasOptions = false;
        for (int i = adjacentDirections.length; --i >= 0; ) {
            if (rc.canMove(adjacentDirections[i])) {
                hasOptions = true;
                break;
            }
        }

        if (!hasOptions) {
            return null;
        }

        int distanceToTarget = chebyshevDist(myLocation, target);
        if (distanceToTarget < minDistanceToTarget) {
            reset();
            minDistanceToTarget = distanceToTarget;
        }
        if (currentObstacle != null
                && rc.canSenseLocation(currentObstacle)
                && rc.sensePassability(currentObstacle)) {
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

    public static void reset() {
        currentTarget = null;
        minDistanceToTarget = Integer.MAX_VALUE;
        obstacleOnRight = true;
        currentObstacle = null;
        visitedStates = new FastSet();
    }

    private static void setInitialDirection() throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction forward = myLoc.directionTo(currentTarget);

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
 
        MapLocation leftLoc = rc.adjacentLocation(left);
        MapLocation rightLoc = rc.adjacentLocation(right);
        int leftDist = chebyshevDist(leftLoc, currentTarget);
        int rightDist = chebyshevDist(rightLoc, currentTarget);

        if (leftDist < rightDist ||
                (leftDist == rightDist
                && myLoc.distanceSquaredTo(leftLoc) < myLoc.distanceSquaredTo(rightLoc))) {
            obstacleOnRight = true;
            currentObstacle = rc.adjacentLocation(left.rotateRight());
        } else {
            obstacleOnRight = false;
            currentObstacle = rc.adjacentLocation(right.rotateLeft());
        }
    }

    private static Direction followWall(boolean canRotate) throws GameActionException {
        Direction direction = rc.getLocation().directionTo(currentObstacle);
        for (int i = 8; --i >= 0; ) {
            direction = obstacleOnRight ? direction.rotateLeft() : direction.rotateRight();
            if (rc.canMove(direction)) {
                return direction;
            }
            MapLocation location = rc.adjacentLocation(direction);
            if (canRotate && !rc.onTheMap(location)) {
                obstacleOnRight = !obstacleOnRight;
                return followWall(false);
            }
            if (rc.onTheMap(location) && !rc.sensePassability(location)) {
                currentObstacle = location;
            }
        }
        return null;
    }

    private static char getState(MapLocation target) {
        MapLocation myLocation = rc.getLocation();
        Direction direction = myLocation.directionTo(currentObstacle != null ? currentObstacle : target);
        int rotation = obstacleOnRight ? 1 : 0;

        return (char) ((((myLocation.x << 6) | myLocation.y) << 4) | (direction.ordinal() << 1) | rotation);
    }

    private static int chebyshevDist(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
}
