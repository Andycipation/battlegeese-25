
def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

def chebyshev(x1, y1, x2, y2):
    return max(abs(x1 - x2), abs(y1 - y2))

vision = [(0, 0), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]

up = [(-1, 2), (-1, 1), (0, 2), (0, 1), (1, 2), (1, 1)]
right = [(y, -x) for (x, y) in up]
down = [(y, -x) for (x, y) in right]
left = [(y, -x) for (x, y) in down]
dirs = [
    up, right, down, left
]

print(left)

print("switch ((diff.x + 4) * 9 + (diff.y)) {")
for dx in range(-4, 5):
    for dy in range(-4, 5):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        mask = 0
        for x in range(-1, 2):
            for y in range(-1, 2):
                if (x, y) not in vision:
                    continue
                if chebyshev(dx, dy, x, y) <= 1:
                    mask |= 1 << vision.index((x, y))

        if mask != 0:
            print(f"    case {((dx + 4) * 9 + dy)}: ", end="")
            print(f"attackScore |= {mask}; ",end="")
            print(f"break; // ({dx}, {dy})")
print("}")
