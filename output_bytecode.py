from argparse import ArgumentParser
from pathlib import Path
from datetime import datetime

import subprocess

def main() -> None:
    parser = ArgumentParser()
    parser.add_argument('package_name', help='name of the source package located under src/')
    args = parser.parse_args()

    subprocess.run(['./gradlew', 'build'], check=True)

    class_dir = Path('build/classes/') / args.package_name
    class_files = list(class_dir.rglob('*.class'))
    print('class files:', class_files)

    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    bytecodes_dir = Path('src/') / args.package_name / f'bytecode-{timestamp}'
    bytecodes_dir.mkdir(parents=True, exist_ok=True)

    for class_file in class_files:
        output_file = bytecodes_dir / f'{class_file.stem}.bytecode'
        with open(output_file, 'w') as f:
            subprocess.run(['javap', '-c', str(class_file)], stdout=f)

if __name__ == '__main__':
    main()
