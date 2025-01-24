
def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

dirs = [(0, 0), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]
splash_spots = []
for dx in range(-4, 5):
    for dy in range(-4, 5):
        if (abs(dx) + abs(dy) <= 4):
            splash_spots.append((dx, dy))
print(len(splash_spots))

print(f"splashSpotIndices = new FastSet()")
for (x, y) in splash_spots:
    print(f"splashSpotIndices.add(new MapLocation({x + 4}, {y + 4})); ", end="")
print()

def encode(loc):
    idx = splash_spots.index(loc)
    return (idx // 12, idx % 12)

ENEMY_WEIGHT = 3
EMPTY_WEIGHT = 1

print("if (tile.getPaint().isEnemy()) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {", end="")
for dx in range(-5, 6):
    for dy in range(-5, 6):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        masks = [0, 0, 0, 0]
        for (x, y) in splash_spots:
            if (dist2(x, y, dx, dy) > 2):
                continue
            i, b = encode((x, y))
            masks[i] += (1 << (5 * b)) * ENEMY_WEIGHT
        if masks != [0, 0, 0, 0]:
            print(f"case {(dx + 4) * 9 + (dy + 4)}: ", end="")
            for i, msk in enumerate(masks):
                if msk != 0:
                    print(f"precompPointsMask[{i}] += {msk}L; ",end="")
            print(f"break; ", end="")
print("} }")

print("if (tile.isPassable() && tile.getPaint() == PaintType.EMPTY) { switch ((diff.x + 4) * 9 + (diff.y + 4)) {", end="")
for dx in range(-5, 6):
    for dy in range(-5, 6):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        masks = [0, 0, 0, 0]
        for (x, y) in splash_spots:
            if (dist2(x, y, dx, dy) > 4):
                continue
            i, b = encode((x, y))
            masks[i] += (1 << (5 * b)) * EMPTY_WEIGHT
        if masks != [0, 0, 0, 0]:
            print(f"case {(dx + 4) * 9 + (dy + 4)}: ", end="")
            for i, msk in enumerate(masks):
                if msk != 0:
                    print(f"precompPointsMask[{i}] += {msk}L; ",end="")
            print(f"break; ", end="")

print()
print()

print("int curX = locBeforeTurn.x; int curY = locBeforeTurn.y;")

print("MapLocation[] splashSpots = {",end="")
for i, (x, y) in enumerate(splash_spots):
    print(f"new MapLocation(curX + {x}, curY + {y})", end=",")
print("};")

for i, (x, y) in enumerate(splash_spots):
    print(f"attackable[{i}] = withinBounds(splashSpots[{i}]) && rc.senseMapInfo(splashSpots[{i}]).isPassable()")

print()
print()
for i, (dx, dy) in enumerate(dirs):
    print(f"if (rc.canMove(Direction.DIRECTION_ORDER[{i}]) && !inEnemyTowerRange(locBeforeTurn.add(Direction.DIRECTION_ORDER[{i}]))) {{", end="")
    for j, (x, y) in enumerate(splash_spots):
        if (dist2(x, y, dx, dy) > 4):
            continue;
        print(f"if (precompPoints[{j}] > bestPoints && attackable[{j}]) {{ bestPoints = precompPoints[{j}]; bestMove = Direction.DIRECTION_ORDER[{i}]; bestAttack = {j};}}", end=" ")
    print("}")
