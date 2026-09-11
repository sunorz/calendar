#!/bin/bash

# Update DARK mode drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#81C995" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#F28B82" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_dark_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#81C995" />
    </shape>
</inset>
XML

# Update LIGHT mode drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#188038" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#D93025" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_light_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="#188038" />
    </shape>
</inset>
XML

# Update BASE mode drawables (fallback)
cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#81C995" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#F28B82" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#81C995" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#188038" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#D93025" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="#188038" />
        </shape>
    </item>
</layer-list>
XML
