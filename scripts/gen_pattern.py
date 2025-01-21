
def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

def chebyshev(x1, y1, x2, y2):
    return max(abs(x1 - x2), abs(y1 - y2))

vision = [(0, 0), (-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]
def encode(x, y):
    x += 4
    y += 4
    return (x // 3, 9 * (x % 3) + y)


print("switch (diff.x * 1000 + diff.y) {")
for dx in range(-4, 5):
    for dy in range(-4, 5):
        if dist2(dx, dy, 0, 0) > 20:
            continue
        masks = [0, 0, 0]
        for x in range(-4, 5):
            for y in range(-4, 5):
                if (chebyshev(dx, dy, x, y) <= 2):
                    i, b = encode(x, y)
                    masks[i] |= (1 << b)
        if masks != [0, 0, 0]:
            print(f"    case {dx * 1000 + dy}: ", end="")
            for i, b in enumerate(masks):
                print(f"precompAppearsClear[{i}] |= {b}; ",end="")
            print(f"break; // ({dx}, {dy})")
print("}")
