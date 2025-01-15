from pathlib import Path
import sys


def main() -> None:
    pkg_name = sys.argv[1]
    src_dir = Path('src') / 'template'
    pkg_dir = Path('src') / pkg_name
    pkg_dir.mkdir()
    for p in src_dir.iterdir():
        code = p.read_text().splitlines()
        code[0] = f'package {pkg_name};'
        dest = pkg_dir / p.name
        with dest.open('w') as f:
            f.write('\n'.join(code))
        print(f'copied {p} to {dest}')


if __name__ == '__main__':
    main()
