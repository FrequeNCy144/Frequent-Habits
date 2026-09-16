import subprocess
import collections

p = subprocess.run(['gradle', ':app:compileDebugKotlin'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
lines = p.stdout.split('\n')
by_file = collections.defaultdict(list)

for l in lines:
    if l.startswith('e: file:///app/src/main/java/'):
        path_part = l.split(':')[1].replace('//app/src/main/java/', '')
        msg = ":".join(l.split(':')[3:]).strip()
        line_num = l.split(':')[2]
        by_file[path_part].append((line_num, msg))

for f, errs in sorted(by_file.items()):
    print(f"=== {f} ({len(errs)} errors) ===")
    for ln, msg in errs:
        print(f"  Line {ln}: {msg}")
