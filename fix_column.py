import sys

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "r") as f:
    code = f.read()

code = code.replace(
"""                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {""", 
"""                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {""")

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "w") as f:
    f.write(code)
