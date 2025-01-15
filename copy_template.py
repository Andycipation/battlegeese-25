from argparse import ArgumentParser
from pathlib import Path


def main() -> None:
    parser = ArgumentParser()
    parser.add_argument('source_name', help='name of the source package located under src/')
    parser.add_argument('dest_name', help='name of the package you want to copy to; it will be located under src/')
    args = parser.parse_args()

    dest = str(args.dest_name)

    src_dir = Path('src') / str(args.source_name)
    dest_dir = Path('src') / dest
    dest_dir.mkdir()
    for src_file_path in src_dir.iterdir():
        code = src_file_path.read_text().splitlines()
        code[0] = f'package {dest};'
        dest_path = dest_dir / src_file_path.name
        with dest_path.open('w') as f:
            f.write('\n'.join(code))
        print(f'copied {src_file_path} to {dest_path}')


if __name__ == '__main__':
    main()
