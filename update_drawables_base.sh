#!/bin/bash

# Update SYSTEM drawables (base with fixed width)
cat << 'XML' > app/src/main/res/drawable/bg_widget_today.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="@color/widget_today_bg_workday" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_red.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="@color/widget_today_bg_restday" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="@color/widget_today_bg_locale" />
        </shape>
    </item>
</layer-list>
XML

# Update DARK mode drawables
cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#81C995" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#F28B82" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_dark_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#81C995" />
        </shape>
    </item>
</layer-list>
XML

# Update LIGHT mode drawables
cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_workday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#188038" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_restday.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#D93025" />
        </shape>
    </item>
</layer-list>
XML

cat << 'XML' > app/src/main/res/drawable/bg_widget_today_light_locale.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="2dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:height="3dp" android:width="24dp" />
            <corners android:radius="1.5dp" />
            <solid android:color="#188038" />
        </shape>
    </item>
</layer-list>
XML

