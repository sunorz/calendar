import sys

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "r") as f:
    code = f.read()

target = """                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {"""
                    
replacement = """                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {"""

# Only replace the one inside TodayPatternPickerDialog
code = code.replace(target, replacement)

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "w") as f:
    f.write(code)
