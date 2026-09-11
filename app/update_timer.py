import re

file_path = "./app/src/main/java/com/example/MainActivity.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace single-line BasicTextField block with formatted Kotlin code
pattern = r"BasicTextField\(\s*value = tempTimerInput.*?innerTextField\(\)\s*\}\s*\}\s*\)"

replacement = """BasicTextField(
                                                    value = tempTimerInput,
                                                    onValueChange = { input ->
                                                        if (input.all { it.isDigit() || it == ':' } && input.length <= 8) {
                                                            tempTimerInput = input
                                                        }
                                                    },
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Ascii,
                                                        imeAction = ImeAction.Done
                                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = {
                                                            val parsed = parseTimeToSeconds(tempTimerInput, timerDurationSeconds)
                                                            if (parsed > 0) timerDurationSeconds = parsed
                                                            isEditingTimerDuration = false
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                        }
                                                    ),
                                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                                        color = TextPrimary,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 28.sp
                                                    ),
                                                    cursorBrush = SolidColor(habitColor),
                                                    modifier = Modifier
                                                        .width(140.dp)
                                                        .focusRequester(focusRequester),
                                                    decorationBox = { innerTextField ->
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            if (tempTimerInput.isEmpty()) {
                                                                Text(
                                                                    text = timeText,
                                                                    color = TextSecondary.copy(alpha = 0.35f),
                                                                    style = MaterialTheme.typography.titleLarge,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 28.sp,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    }
                                                )"""

new_content, count = re.subn(pattern, replacement, content, flags=re.DOTALL)
print(f"Substitutions made: {count}")

if count > 0:
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("Successfully updated MainActivity.kt")
else:
    print("Pattern not found!")
