package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Shared source of all launchable apps for Home and the full-screen app library. */
final class AppLibraryCatalog {
    static final class Entry {
        final ResolveInfo resolveInfo;
        final String packageName;
        final String label;
        final boolean leanback;
        final long installedAt;

        Entry(ResolveInfo resolveInfo, String packageName, String label, boolean leanback, long installedAt) {
            this.resolveInfo = resolveInfo;
            this.packageName = packageName;
            this.label = label;
            this.leanback = leanback;
            this.installedAt = installedAt;
        }

        Entry(String packageName, String label, boolean leanback, long installedAt) {
            this(null, packageName, label, leanback, installedAt);
        }
    }

    private AppLibraryCatalog() { }

    static List<Entry> load(Context context, Set<String> homePackages) {
        PackageManager packageManager = context.getPackageManager();
        List<Entry> leanback = entries(context, packageManager, Intent.CATEGORY_LEANBACK_LAUNCHER, true);
        List<Entry> launcher = entries(context, packageManager, Intent.CATEGORY_LAUNCHER, false);
        return mergeAndSort(leanback, launcher, homePackages, System.currentTimeMillis());
    }

    static List<Entry> mergeAndSort(List<Entry> leanback, List<Entry> launcher,
            Set<String> homePackages, long now) {
        Map<String, Entry> unique = new LinkedHashMap<>();
        for (Entry entry : leanback) unique.putIfAbsent(entry.packageName, entry);
        for (Entry entry : launcher) unique.putIfAbsent(entry.packageName, entry);
        List<Entry> catalog = new ArrayList<>(unique.values());
        Collator collator = Collator.getInstance();
        Collections.sort(catalog, (first, second) -> {
            int priority = LibraryApps.comparePriority(homePackages.contains(first.packageName), first.installedAt,
                    homePackages.contains(second.packageName), second.installedAt, now);
            if (priority != 0) return priority;
            int byLabel = collator.compare(first.label, second.label);
            return byLabel != 0 ? byLabel : first.packageName.compareTo(second.packageName);
        });
        return catalog;
    }

    static List<Entry> filter(List<Entry> catalog, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) return new ArrayList<>(catalog);
        String needle = trimmed.toLowerCase(Locale.getDefault());
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : catalog) {
            if (entry.label.toLowerCase(Locale.getDefault()).contains(needle)) matches.add(entry);
        }
        return matches;
    }

    static HashSet<String> withHomeState(Set<String> homePackages, String packageName, boolean onHome) {
        HashSet<String> updated = new HashSet<>(homePackages);
        if (onHome) updated.add(packageName); else updated.remove(packageName);
        return updated;
    }

    static String selectionAfterFilter(List<Entry> catalog, String selectedPackage) {
        for (Entry entry : catalog) if (entry.packageName.equals(selectedPackage)) return selectedPackage;
        return catalog.isEmpty() ? null : catalog.get(0).packageName;
    }

    private static List<Entry> entries(Context context, PackageManager packageManager, String category,
            boolean leanback) {
        List<Entry> result = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            if (info.activityInfo == null || !info.activityInfo.exported) continue;
            String packageName = info.activityInfo.packageName;
            if (packageName.equals(context.getPackageName())) continue;
            String label;
            try {
                label = Cards.cleanLabel(String.valueOf(info.loadLabel(packageManager)), packageName);
            } catch (RuntimeException unavailable) {
                label = packageName;
            }
            long installedAt;
            try {
                installedAt = packageManager.getPackageInfo(packageName, 0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
                installedAt = 0L;
            }
            result.add(new Entry(info, packageName, label, leanback, installedAt));
        }
        return result;
    }
}
