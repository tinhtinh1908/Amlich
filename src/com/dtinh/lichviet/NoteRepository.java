package com.dtinh.lichviet;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public final class NoteRepository {
    private static final int MAX_LENGTH = 240;
    private static final String PREFS = "calendar_notes_v1";

    private NoteRepository() {
    }

    public static String get(Context context, Calendar calendar) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(calendar), "").trim();
    }

    public static boolean has(Context context, Calendar calendar) {
        return !get(context, calendar).isEmpty();
    }

    static boolean has(Set<String> noteKeys, Calendar calendar) {
        return noteKeys.contains(key(calendar));
    }

    static Set<String> snapshotKeys(Context context) {
        Map<String, ?> all = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll();
        HashSet<String> keys = new HashSet<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (isNoteKey(entry.getKey()) && value instanceof String
                    && !((String) value).trim().isEmpty()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public static void put(Context context, Calendar calendar, String value) {
        String note = sanitize(value);
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (note.isEmpty()) {
            editor.remove(key(calendar));
        } else {
            editor.putString(key(calendar), note);
        }
        editor.apply();
        ThemeManager.refreshWidgets(context.getApplicationContext());
    }

    public static Map<String, String> exportAll(Context context) {
        Map<String, ?> all = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll();
        TreeMap<String, String> notes = new TreeMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (isNoteKey(entry.getKey()) && (value instanceof String)) {
                String note = sanitize((String) value);
                if (!note.isEmpty()) {
                    notes.put(entry.getKey(), note);
                }
            }
        }
        return notes;
    }

    public static int mergeAll(Context context, Map<String, String> map) {
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        int mergedCount = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (isNoteKey(entry.getKey())) {
                String note = sanitize(entry.getValue());
                if (!note.isEmpty()) {
                    editor.putString(entry.getKey(), note);
                    mergedCount++;
                }
            }
        }
        if (mergedCount > 0) {
            editor.apply();
            ThemeManager.refreshWidgets(context.getApplicationContext());
        }
        return mergedCount;
    }

    private static String key(Calendar calendar) {
        return String.format(Locale.US, "note_%04d%02d%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean isNoteKey(String key) {
        if (key == null || key.length() != 13 || !key.startsWith("note_")) {
            return false;
        }
        for (int index = 5; index < key.length(); index++) {
            char character = key.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static String sanitize(String value) {
        String note = value == null ? "" : value.trim();
        return note.length() <= MAX_LENGTH
                ? note : note.substring(0, MAX_LENGTH).trim();
    }

}
