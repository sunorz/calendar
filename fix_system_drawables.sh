#!/bin/bash

# Update SYSTEM mode drawables (v26 with percentages)
cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="@color/widget_today_bg_workday" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_red.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="@color/widget_today_bg_restday" />
    </shape>
</inset>
XML

cat << 'XML' > app/src/main/res/drawable-v26/bg_widget_today_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="25%"
    android:insetRight="25%"
    android:insetBottom="3dp">
    <shape android:shape="rectangle">
        <size android:height="4dp" />
        <corners android:radius="2dp" />
        <solid android:color="@color/widget_today_bg_locale" />
    </shape>
</inset>
XML

# Update SYSTEM mode drawables (fallback)
cat << 'XML' > app/src/main/res/drawable/bg_widget_today.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="@color/widget_today_bg_workday" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_red.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="@color/widget_today_bg_restday" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="3dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="4dp" android:width="16dp" />
            <corners android:radius="2dp" />
            <solid android:color="@color/widget_today_bg_locale" />
        </shape>
    </item>
</layer-list>
XML
