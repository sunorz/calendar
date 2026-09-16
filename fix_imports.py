import sys

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "r") as f:
    lines = f.readlines()

imports_to_add = [
    "import androidx.compose.ui.graphics.Color\n",
    "import androidx.compose.ui.graphics.Brush\n",
    "import androidx.compose.runtime.LaunchedEffect\n",
    "import androidx.compose.runtime.mutableFloatStateOf\n",
    "import androidx.compose.runtime.mutableIntStateOf\n",
    "import androidx.compose.foundation.layout.wrapContentSize\n",
    "import androidx.compose.foundation.layout.BoxWithConstraints\n",
    "import androidx.compose.foundation.layout.width\n",
    "import androidx.compose.foundation.layout.offset\n"
]

insert_idx = 0
for i, line in enumerate(lines):
    if line.startswith("import "):
        insert_idx = i

for imp in imports_to_add:
    if imp not in lines:
        lines.insert(insert_idx + 1, imp)

with open("app/src/main/java/com/sunplace/calendar/ui/SettingsDialog.kt", "w") as f:
    f.writelines(lines)
