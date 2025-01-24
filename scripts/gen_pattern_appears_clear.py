
def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

def chebyshev(x1, y1, x2, y2):
    return max(abs(x1 - x2), abs(y1 - y2))

vision = [(0, 0), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]

up = [(-1, 2), (-1, 1), (0, 2), (0, 1), (1, 2), (1, 1)]
right = [(y, -x) for (x, y) in up]
down = [(y, -x) for (x, y) in right]
left = [(y, -x) for (x, y) in down]

print("switch ((diff.x + 4) * 9 + (diff.y + 4)) {")
for dx in range(-4, 5):
    for dy in range(-4, 5):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        masks = [0, 0, 0]
        for x in range(-4, 5):
            for y in range(-4, 5):
                if (dist2(x, y, 0, 0) > 20):
                    continue
                if (chebyshev(dx, dy, x, y) <= 2):
                    masks[(x+4) / 3] |= (1 << (((x+4) % 3) * 9 + (y+4)))
        if masks != [0, 0, 0]:
            print(f"    case {(dx + 4) * 9 + dy + 4}: ", end="")
            print(f"inEnemyTowerRangeMask |= {mask}; ",end="")
            print(f"break; // ({dx}, {dy})")
print("}")
