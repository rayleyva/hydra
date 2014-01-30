/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.store.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;

public class SettingsJE extends SettingsDB {

    public static final String JE_READ_ONLY = "cs.je.readOnly";
    public static final String JE_CACHE_SIZE = "cs.je.cacheSize";
    public static final String JE_DEFERRED_WRITE = "cs.je.deferredWrite";
    public static final String JE_LOG_MINUSED = "cs.je.logMinUsed";
    public static final String JE_LOGFILE_MINUSED = "cs.je.logFileMinUsed";

    private static final Collection<String> setSet = Arrays.asList(JE_READ_ONLY,
            JE_CACHE_SIZE,
            JE_DEFERRED_WRITE);

    public static Map<String, Object> dumpState(Database db) {
        TreeMap<String, Object> map = new TreeMap<String, Object>();
        DatabaseConfig dc = db.getConfig();
        EnvironmentConfig ec = db.getEnvironment().getConfig();
        map.put("cacheMode", dc.getCacheMode());
        map.put("deferredWrite", dc.getDeferredWrite());
        map.put("keyPrefix", dc.getKeyPrefixing());
        map.put("nodeMax", dc.getNodeMaxEntries());
        map.put("nodeMaxDup", dc.getNodeMaxDupTreeEntries());
        map.put("cachePercent", ec.getCachePercent());
        map.put("cacheSize", ec.getCacheSize());
        map.put("durability", ec.getDurability());
        map.put("readOnly", ec.getReadOnly());
        map.put("sharedCache", ec.getSharedCache());
        map.put("transactional", ec.getTransactional());
        map.put("logUtilization", ec.getConfigParam(EnvironmentConfig.CLEANER_MIN_UTILIZATION));
        map.put("logFileUtilization", ec.getConfigParam(EnvironmentConfig.CLEANER_MIN_FILE_UTILIZATION));
        return map;
    }

    public static String dumpDebug(Database db) {
        LinkedList<String> list = new LinkedList<String>();
        for (Entry<String, Object> e : dumpState(db).entrySet()) {
            list.add("[" + e.getKey() + "=" + e.getValue() + "]");
        }
        return list.toString();
    }

    @Override
    public Collection<String> getSupportedKeys() {
        return setSet;
    }

    public static void updateEnvironmentConfig(SettingsDB settings, EnvironmentConfig eco) {
        if (settings.hasValue(JE_READ_ONLY)) eco.setReadOnly(settings.isTrue(JE_READ_ONLY));
        String cacheSize = settings.getValue(JE_CACHE_SIZE);
        if (cacheSize != null) {
            if (cacheSize.endsWith("%")) {
                eco.setCachePercent(Integer.parseInt(cacheSize.substring(0, cacheSize.length() - 1)));
            } else {
                eco.setCacheSize(parseNumber(cacheSize));
            }
        }
        if (settings.hasValue(JE_LOG_MINUSED)) {
            eco.setConfigParam(EnvironmentConfig.CLEANER_MIN_UTILIZATION, settings.getValue(JE_LOG_MINUSED));
        }
        if (settings.hasValue(JE_LOGFILE_MINUSED)) {
            eco.setConfigParam(EnvironmentConfig.CLEANER_MIN_FILE_UTILIZATION, settings.getValue(JE_LOGFILE_MINUSED));
        }
    }

    public static void updateDatabaseConfig(SettingsDB settings, DatabaseConfig config) {
        if (settings.hasValue(JE_DEFERRED_WRITE)) config.setDeferredWrite(settings.isTrue(JE_DEFERRED_WRITE));
    }

    /**
     * Sets JE EnvironmentConfig params using Java System paremeters
     */
    public static String mergeSystemProperties(EnvironmentConfig eco) {
        // load properties from system je.*
        Properties prop = System.getProperties();
        StringBuilder sb = new StringBuilder();
        for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (!key.startsWith("je.")) {
                continue;
            }
            String val = prop.getProperty(key);
            sb.append(key).append("=").append(val).append(" ");
            eco.setConfigParam(key, val);
        }
        return sb.toString();
    }
}
