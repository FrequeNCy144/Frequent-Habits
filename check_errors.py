import subprocess
import os
import re

def get_errors():
    p = subprocess.run(['gradle', ':app:compileDebugKotlin'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    lines = p.stdout.split('\n')
    errs = [l for l in lines if l.startswith('e: ')]
    return errs

errs = get_errors()
print(f"Total error lines: {len(errs)}")
for e in errs[:35]:
    print(e)
