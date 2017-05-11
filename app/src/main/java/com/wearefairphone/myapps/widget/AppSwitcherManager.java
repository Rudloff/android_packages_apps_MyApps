/*
 * Copyright (C) 2013 Fairphone Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wearefairphone.myapps.widget;


import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.wearefairphone.myapps.appinfo.ApplicationRunInfoManager;
import com.wearefairphone.myapps.appinfo.ApplicationRunInformation;
import com.wearefairphone.myapps.utils.UsageStatsHelper;

import java.util.ArrayList;
import java.util.List;

class AppSwitcherManager {
    private static final String TAG = AppSwitcherManager.class.getSimpleName();
    private static final String PREFS_APP_SWITCHER_APPS_DATA = "com.fairphone.fplauncher3.PREFS_APP_SWITCHER_APPS_DATA";

    private static final ApplicationRunInfoManager _instance = new ApplicationRunInfoManager(true);

    static synchronized ApplicationRunInfoManager getInstance() {
        return _instance;
    }

    private static BroadcastReceiver sBCastLaunchAllApps;
    private static BroadcastReceiver sBCastAppLauncher;

    public static void unregisterAppSwitcherBroadcastReceivers(Context context) {
        Log.d(TAG, "unregisterAppSwitcherBroadcastReceivers");
        if (sBCastLaunchAllApps != null) {
            context.unregisterReceiver(sBCastLaunchAllApps);
            sBCastLaunchAllApps = null;
        }
        if (sBCastAppLauncher != null) {
            context.unregisterReceiver(sBCastAppLauncher);
            sBCastAppLauncher = null;
        }
    }

//	public static void registerAppSwitcherBroadcastReceivers(Launcher launcher) {
//		Log.d(TAG, "registerAppSwitcherBroadcastReceivers");
//		if (sBCastLaunchAllApps == null) {
//			// launching the application
//			sBCastLaunchAllApps = new BroadcastReceiver() {
//				private Launcher launcher;
//
//				protected BroadcastReceiver setup(Launcher l) {
//					launcher = l;
//					return this;
//				}
//
//				@Override
//				public void onReceive(Context context, Intent intent) {
//					Log.d(TAG, "Received a call from widget....Show all apps");
//					launcher.showAllAppsDrawer();
//				}
//			}.setup(launcher);
//			launcher.registerReceiver(sBCastLaunchAllApps, new IntentFilter(
//					AppSwitcherWidget.ACTION_APP_SWITCHER_LAUNCH_ALL_APPS));
//		}
//
//		if (sBCastAppLauncher == null) {
//			sBCastAppLauncher = new BroadcastReceiver() {
//				private Launcher launcher;
//
//				protected BroadcastReceiver setup(Launcher l) {
//					launcher = l;
//					return this;
//				}
//
//				@Override
//				public void onReceive(Context context, Intent intent) {
//
//					String packageName = intent
//							.getStringExtra(AppSwitcherWidget.EXTRA_LAUNCH_APP_PACKAGE);
//					String className = intent
//							.getStringExtra(AppSwitcherWidget.EXTRA_LAUNCH_APP_CLASS_NAME);
//
//					Log.d(TAG, "Received a call from widget....Launch App "
//							+ packageName + " - " + className);
//
//					Intent launchIntent = context.getPackageManager()
//							.getLaunchIntentForPackage(packageName);
//
//					if (launchIntent != null) {
//						launchIntent.setComponent(new ComponentName(packageName,
//								className));
//						launcher.launchActivity(null, launchIntent,
//								"AppSwitcherLaunch");
//					}
//				}
//			}.setup(launcher);
//			launcher.registerReceiver(sBCastAppLauncher, new IntentFilter(
//					AppSwitcherWidget.ACTION_APP_SWITCHER_LAUNCH_APP));
//		}
//
//	}

    private static void saveAppSwitcherData(Context context) {
        Log.d(TAG, "saveAppSwitcherData");
        ApplicationRunInformation.persistAppRunInfo(context,
                PREFS_APP_SWITCHER_APPS_DATA, AppSwitcherManager.getInstance()
                        .getAllAppRunInfo());
    }

    static void loadAppSwitcherData(Context context) {

        // Most Used
        Log.d(TAG, "loadAppSwitcherData ");
        AppSwitcherManager.getInstance().resetState();
        AppSwitcherManager.getInstance().setAllRunInfo(generateAppInfo(context));
    }

    private static List<ApplicationRunInformation> generateAppInfo(final Context context) {

        UsageStatsHelper helper = new UsageStatsHelper();

        if (helper.hasPermission(context)) {
            return helper.getUsageStats(context);
        } else {
            Log.d(TAG, "Requesting permission to access usage stats");
//            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);
        }
        return new ArrayList<>();

    }

    public static void updateAppSwitcherData(Context context, ArrayList<String> packageNames) {
        List<ApplicationRunInformation> allApps = AppSwitcherManager.getInstance().getAllAppRunInfo();

        List<ApplicationRunInformation> appsToRemove = new ArrayList<>();

        for (ApplicationRunInformation appRunInfo : allApps) {
            if (packageNames.contains(appRunInfo.getComponentName().getPackageName())) {
                appsToRemove.add(appRunInfo);
            }
        }

        for (ApplicationRunInformation appToRemove : appsToRemove) {
            applicationRemoved(context, appToRemove.getComponentName());
        }
    }

    public static void updateAppSwitcherWidgets(Context context) {
        Log.d(TAG, "updateAppSwitcherWidgets");
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        int[] appWidgetIds = appWidgetManager
                .getAppWidgetIds(new ComponentName(context,
                        AppSwitcherWidget.class));
        if (appWidgetIds.length > 0) {
            new AppSwitcherWidget().onUpdate(context, appWidgetManager,
                    appWidgetIds);
        }
    }

    public static void applicationStarted(Context context, ComponentName componentName) {
        ApplicationRunInformation appRunInfo = ApplicationRunInfoManager
                .generateApplicationRunInfo(componentName, false);
        AppSwitcherManager.getInstance().applicationStarted(appRunInfo);
        saveAppSwitcherData(context);
        updateAppSwitcherWidgets(context);
    }

    public static void applicationRemoved(Context context, ComponentName componentName) {
        AppSwitcherManager.getInstance().applicationRemoved(componentName);
        saveAppSwitcherData(context);
        updateAppSwitcherWidgets(context);
    }
}
