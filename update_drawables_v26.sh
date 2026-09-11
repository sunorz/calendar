#!/bin/bash

# Update SYSTEM drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="@color/widget_today_bg_workday" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_red.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="@color/widget_today_bg_restday" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="@color/widget_today_bg_locale" />
    </shape>
</inset>
XML

# Update DARK mode drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#81C995" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#F28B82" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#81C995" />
    </shape>
</inset>
XML

# Update LIGHT mode drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#188038" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#D93025" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="15%"
    android:insetRight="15%"
    android:insetBottom="2dp">
    <shape android:shape="rectangle">
        <size android:height="3dp" />
        <corners android:radius="1.5dp" />
        <solid android:color="#188038" />
    </shape>
</inset>
XML

