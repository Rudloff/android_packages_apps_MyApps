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
package com.wearefairphone.myapps.appinfo;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class processes the count for the most used apps and the most recent.
 *
 * @author Tiago Costa
 */
public class ApplicationRunInfoManager {

    public static final int RECENT_APP_MAX_COUNT_LIMIT = 5;
    public static final int MOST_APP_MAX_COUNT_LIMIT = 5;
    private static final int MINIMAL_COUNT = 2;
    private static final String TAG = ApplicationRunInfoManager.class.getSimpleName();

    private LimitedQueue<ApplicationRunInformation> _mostUsed;
    private LimitedQueue<ApplicationRunInformation> _recentApps;
    private Map<String, ApplicationRunInformation> _appRunInfos;

    private int _mostUsedAppsLimit;
    private int _recentAppsLimit;
    private final boolean _updateLists;

    public ApplicationRunInfoManager(boolean updateLists) {
        this._updateLists = updateLists;
        _appRunInfos = new HashMap<>();
        if (_updateLists) {
            setUpLimits(MOST_APP_MAX_COUNT_LIMIT, RECENT_APP_MAX_COUNT_LIMIT);
        }
    }

    public static ApplicationRunInformation generateApplicationRunInfo(ComponentName component, boolean isFreshInstall, boolean isUpdated) {
        ApplicationRunInformation appInfo = new ApplicationRunInformation(component);
        appInfo.incrementCount();
        appInfo.setLastExecution(Calendar.getInstance().getTime());
        appInfo.setIsNewApp(isFreshInstall);
        appInfo.setIsPinnedApp(false);
        appInfo.setIsUpdatedApp(isUpdated);

        return appInfo;
    }

    public static ApplicationRunInformation generateApplicationRunInfo(ComponentName component, boolean isFreshInstall) {
        return generateApplicationRunInfo(component, isFreshInstall, false);
    }

    public void setUpLimits(int maxMostUsed, int maxRecentApps) {
        _mostUsedAppsLimit = maxMostUsed;
        _recentAppsLimit = maxRecentApps;

        // refactor the limits
        setUpNewLimits();
    }

    private void setUpNewLimits() {
        _mostUsed = new LimitedQueue<>(_mostUsedAppsLimit);
        _recentApps = new LimitedQueue<>(_recentAppsLimit);

        // update the information
        if (_appRunInfos != null) {
            updateAppInformation();
        }
    }

    public void loadNewRunInformation(List<ApplicationRunInformation> allApps) {
        // clear the current state
        resetState();

        // add application to the bag
        for (ApplicationRunInformation appInfo : allApps) {
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()), appInfo);
        }

        // update the information
        updateAppInformation();
    }

    public void resetState() {
        if (_updateLists) {
            _mostUsed.clear();
            _recentApps.clear();
        }
        _appRunInfos.clear();
    }

    public void applicationStarted(ApplicationRunInformation appInfo) {
        // obtain the cached app information
        ApplicationRunInformation cachedApp = _appRunInfos.get(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()));
        // if does not exist, create one
        if (cachedApp == null) {
            Log.d(TAG, "No entry yet");
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()), appInfo);

            cachedApp = appInfo;

            cachedApp.resetCount();
        }

        // increment count
        cachedApp.incrementCount();

        Log.d(TAG, "Logging application : " + cachedApp.getComponentName() + " : " + cachedApp.getCount());

        // set the current time for the last execution
        cachedApp.setLastExecution(appInfo.getLastExecution());

        cachedApp.setIsNewApp(false);
        cachedApp.setIsUpdatedApp(false);
        cachedApp.setIsPinnedApp(appInfo.isPinnedApp());

        // update the informations
        updateAppInformation();
    }

    public boolean applicationPinned(ApplicationRunInformation appInfo) {
        // obtain the cached app information
        ApplicationRunInformation cachedApp = _appRunInfos.get(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()));
        // if does not exist, create one
        if (cachedApp == null) {
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()), appInfo);

            cachedApp = appInfo;
            cachedApp.setIsPinnedApp(false);
            cachedApp.setIsNewApp(false);
            cachedApp.setIsUpdatedApp(false);
            cachedApp.resetCount();
        }
        cachedApp.setIsPinnedApp(!cachedApp.isPinnedApp());
        return cachedApp.isPinnedApp();
    }

    public void applicationInstalled(ApplicationRunInformation appInfo) {
        // obtain the cached app information
        ApplicationRunInformation cachedApp = _appRunInfos.get(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()));
        // if does not exist, create one
        if (cachedApp == null) {
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()), appInfo);

            cachedApp = appInfo;

            cachedApp.resetCount();
        }

        Log.d(TAG, "Logging application : " + cachedApp.getComponentName() + " : " + cachedApp.getCount());

        // set the current time for the last execution
        cachedApp.setLastExecution(appInfo.getLastExecution());

        cachedApp.setIsNewApp(true);
        cachedApp.setIsUpdatedApp(false);
        cachedApp.setIsPinnedApp(false);
    }

    public void applicationUpdated(ApplicationRunInformation appInfo) {
        // obtain the cached app information
        ApplicationRunInformation cachedApp = _appRunInfos.get(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()));
        // if does not exist, create one
        if (cachedApp == null) {
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(appInfo.getComponentName()), appInfo);

            cachedApp = appInfo;

            cachedApp.resetCount();
        }

        Log.d(TAG, "Logging application : " + cachedApp.getComponentName() + " : " + cachedApp.getCount());
        cachedApp.setIsNewApp(false);
        cachedApp.setIsUpdatedApp(true);
    }

    public ApplicationRunInformation getApplicationRunInformation(Context context, ComponentName componentName) {
        // obtain the cached app information
        ApplicationRunInformation cachedApp = _appRunInfos.get(ApplicationRunInformation.serializeComponentName(componentName));
        //update age
        if (cachedApp != null) {
            updateAgeInfo(context, cachedApp);
        }

        return cachedApp;
    }

    private static void updateAgeInfo(Context context, ApplicationRunInformation appRunInfo) {
        if (appRunInfo != null) {
            Date now = Calendar.getInstance().getTime();
            long timePastSinceLastExec = now.getTime() - appRunInfo.getLastExecution().getTime();
            boolean isPinned = appRunInfo.isPinnedApp();

            if (timePastSinceLastExec < ApplicationRunInformation.getAgeLevelInMiliseconds(context, ApplicationRunInformation.APP_AGE.FREQUENT_USE) || isPinned) {
                appRunInfo.setAge(ApplicationRunInformation.APP_AGE.FREQUENT_USE);
            } else {
                appRunInfo.setAge(ApplicationRunInformation.APP_AGE.RARE_USE);
            }
        }
    }

    public void applicationRemoved(ComponentName component) {
        // remove data
        ApplicationRunInformation appInfo = _appRunInfos.remove(ApplicationRunInformation.serializeComponentName(component));

        // if does not exist return
        if (appInfo == null) {
            return;
        }

        // if its being used in the lists refactor the lists
        if (_updateLists) {
            if (_mostUsed.contains(appInfo) || _recentApps.contains(appInfo)) {
                updateAppInformation();
            }
        }
    }

    private void updateAppInformation() {
        if (_updateLists) {
            _mostUsed.clear();
            _recentApps.clear();

            // most used
            // calculate the most used
            for (ApplicationRunInformation current : _appRunInfos.values()) {

                if (current.getCount() >= MINIMAL_COUNT) {
                    addByCount(current, _mostUsed, _mostUsedAppsLimit);
                }
            }

            printMostUsedApps();

            // calculate the most recent
            for (ApplicationRunInformation current : _appRunInfos.values()) {
                if (!_mostUsed.contains(current)) {
                    addByDate(current, _recentApps, _recentAppsLimit);
                }
            }

            printRecentApps();
        }
    }

    private void printRecentApps() {
        for (ApplicationRunInformation current : _recentApps) {
            Log.d(TAG, "Fairphone RecentApps - " + current);
        }
    }

    private void printMostUsedApps() {
        for (ApplicationRunInformation current : _mostUsed) {
            Log.d(TAG, "Fairphone MostUsed - " + current);
        }
    }

    private static void addByDate(ApplicationRunInformation info, LimitedQueue<ApplicationRunInformation> queue, int limit) {
        for (int insertIdx = 0; insertIdx < queue.size(); insertIdx++) {
            if (queue.get(insertIdx).getLastExecution().before(info.getLastExecution())) {
                queue.add(insertIdx, info);

                return;
            }
        }

        if (queue.size() < limit) {
            queue.addLast(info);
        }
    }

    private static void addByCount(ApplicationRunInformation info, LimitedQueue<ApplicationRunInformation> queue, int limit) {
        for (int insertIdx = 0; insertIdx < queue.size(); insertIdx++) {
            Log.d(TAG, "Fairphone - Contacting ... " + queue.get(insertIdx));
            if (info.getCount() > queue.get(insertIdx).getCount()) {
                Log.d(TAG, "FairPhone - Qs : " + queue.size() + " : Most Used : Adding " + info.getComponentName() + " to position " + insertIdx);
                queue.add(insertIdx, info);

                return;
            }
        }

        Log.d(TAG, "Fairphone - Qs : " + queue.size() + " : Most Used : Adding " + info.getComponentName() + " to first position ");
        if (queue.size() < limit) {
            queue.addLast(info);
        }
    }

    private static class LimitedQueue<E> extends LinkedList<E> {

        /**
         *
         */
        private static final long serialVersionUID = 8174761694444365605L;
        private final int limit;

        public LimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public void add(int idx, E o) {
            super.add(idx, o);

            while (size() > limit) {
                super.removeLast();
            }
        }

        @Override
        public boolean add(E o) {
            super.addLast(o);
            while (size() > limit) {
                super.removeLast();
            }
            return true;
        }
    }

    public List<ApplicationRunInformation> getRecentApps() {

        Log.d(TAG, "Fairphone - Getting recent apps... " + _recentApps.size());
        return _recentApps;
    }

    public List<ApplicationRunInformation> getMostUsedApps() {
        Log.d(TAG, "Fairphone - Getting most Used apps... " + _mostUsed.size());

        return _mostUsed;
    }

    public int getMostUsedAppsLimit() {
        return _mostUsedAppsLimit;
    }

    public int getRecentAppsLimit() {
        return _recentAppsLimit;
    }

    public List<ApplicationRunInformation> getAllAppRunInfo() {
        return new ArrayList<>(_appRunInfos.values());
    }

    public void setAllRunInfo(List<ApplicationRunInformation> allApps) {
        if (_updateLists) {
            resetState();
        }

        for (ApplicationRunInformation app : allApps) {
            _appRunInfos.put(ApplicationRunInformation.serializeComponentName(app.getComponentName()), app);
        }

        updateAppInformation();
    }

}
