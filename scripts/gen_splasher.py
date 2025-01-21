
def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

dirs = [(0, 0), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]

print("switch (diff.x * 10 + diff.y) {")
for dx in range(-5, 6):
    for dy in range(-5, 6):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        tiles = []
        for x in range(-2, 3):
            for y in range(-2, 3):
                if (dist2(x, y, 0, 0) > 2):
                    continue
                if (dist2(dx, dy, x, y) <= 4):
                    tiles.append((x, y))
        if tiles:
            print(f"    case {dx * 100 + dy}: ", end="")
            for (x, y) in tiles:
                print(f"points[{x+2}][{y+2}] += emptyWeight; ",end="")
            print(f"break; // ({dx}, {dy})")
print("}")

print("switch (diff.x * 10 + diff.y) {")
for dx in range(-5, 6):
    for dy in range(-5, 6):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        tiles = []
        for x in range(-2, 2):
            for y in range(-2, 2):
                if (dist2(x, y, 0, 0) > 4):
                    continue
                if (dist2(dx, dy, x, y) <= 2):
                    tiles.append((x, y))
        if tiles:
            print(f"    case {dx * 100 + dy}: ", end="")
            for (x, y) in tiles:
                print(f"points[{x+2}][{y+2}] += enemyWeight; ",end="")
            print(f"break; // ({dx}, {dy})")

print("}")
