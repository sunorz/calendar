#!/bin/bash

# Base template function
create_pill() {
    local file=$1
    local color=$2
    cat << XML > "$file"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:bottom="0dp" android:gravity="bottom|center_horizontal">
        <shape android:shape="rectangle">
            <size android:width="16dp" android:height="4dp" />
            <corners android:radius="2dp" />
            <solid android:color="$color" />
        </shape>
    </item>
</layer-list>
XML
}

DIR="app/src/main/res/drawable"

create_pill "$DIR/bg_widget_today.xml" "@color/widget_today_bg_workday"
create_pill "$DIR/bg_widget_today_red.xml" "@color/widget_today_bg_restday"
create_pill "$DIR/bg_widget_today_locale.xml" "@color/widget_today_bg_locale"

create_pill "$DIR/bg_widget_today_dark_workday.xml" "#81C995"
create_pill "$DIR/bg_widget_today_dark_restday.xml" "#F28B82"
create_pill "$DIR/bg_widget_today_dark_locale.xml" "#81C995"

create_pill "$DIR/bg_widget_today_light_workday.xml" "#188038"
create_pill "$DIR/bg_widget_today_light_restday.xml" "#D93025"
create_pill "$DIR/bg_widget_today_light_locale.xml" "#188038"

