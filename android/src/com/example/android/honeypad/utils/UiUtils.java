/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.honeypad.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

public class UiUtils {

    public static boolean atLeastHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions of the OS since they are inlined at compile time. This is
        // guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean atLeastICS() {
        // Can use static final constants like ICE_CREAM_SANDWICH, declared in
        // later versions of the OS since they are inlined at compile time. This
        // is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return atLeastHoneycomb() && isTablet(context);
    }

    public static void setActivatedCompat(View view, boolean activated) {
        if (atLeastHoneycomb()) {
            view.setActivated(activated);
        }
    }

}
