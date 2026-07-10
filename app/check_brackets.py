def find_mismatched_brackets(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    stack = []
    for index, char in enumerate(content):
        if char == '{':
            stack.append((index, char))
        elif char == '}':
            if not stack:
                print(f"Extra closing bracket '}}' at index {index}")
                return
            stack.pop()
            
    if stack:
        print(f"Unmatched opening brackets: {len(stack)}")
        # Print lines of the last few open brackets
        lines = content.splitlines()
        for idx, char in stack[-5:]:
            # find line number for this index
            char_count = 0
            for line_num, line in enumerate(lines, 1):
                char_count += len(line) + 1 # +1 for newline
                if char_count >= idx:
                    print(f"Opening '{char}' on line {line_num}: {line.strip()[:60]}")
                    break

find_mismatched_brackets('/app/src/main/java/com/example/MainActivity.kt')
