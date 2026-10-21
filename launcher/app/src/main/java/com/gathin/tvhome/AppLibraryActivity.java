package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AppLibraryActivity extends Activity {
    static final String EXTRA_HOME_PACKAGES = "com.gathin.tvhome.extra.HOME_PACKAGES";
    static final int REQUEST_CODE = 73;

    private final HashSet<String> homePackages = new HashSet<>();
    private final List<AppLibraryCatalog.Entry> allApps = new ArrayList<>();
    private final List<AppLibraryCatalog.Entry> visibleApps = new ArrayList<>();
    private RecyclerView grid;
    private EditText filter;
    private TextView empty;
    private CatalogAdapter adapter;
    private PopupWindow actionPopup;
    private String selectedPackage;
    private boolean homeChanged;
    private long catalogTime;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ArrayList<String> packages = getIntent().getStringArrayListExtra(EXTRA_HOME_PACKAGES);
        if (packages != null) homePackages.addAll(packages);
        setContentView(R.layout.activity_app_library);
        grid = findViewById(R.id.appLibraryGrid);
        filter = findViewById(R.id.appLibraryFilter);
        empty = findViewById(R.id.appLibraryEmpty);
        adapter = new CatalogAdapter();
        grid.setLayoutManager(new GridLayoutManager(this, 6));
        grid.setItemAnimator(null);
        grid.setAdapter(adapter);
        findViewById(R.id.appLibraryBack).setOnClickListener(view -> finish());
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable text) { }
        });
        refreshCatalog();
    }

    @Override protected void onResume() {
        super.onResume();
        if (grid != null) refreshCatalog();
    }

    @Override public void onBackPressed() {
        if (actionPopup != null) {
            actionPopup.dismiss();
            return;
        }
        super.onBackPressed();
    }

    private void refreshCatalog() {
        allApps.clear();
        allApps.addAll(AppLibraryCatalog.load(this, homePackages));
        catalogTime = System.currentTimeMillis();
        applyFilter();
    }

    private void applyFilter() {
        visibleApps.clear();
        visibleApps.addAll(AppLibraryCatalog.filter(allApps, filter == null ? "" : filter.getText().toString()));
        selectedPackage = AppLibraryCatalog.selectionAfterFilter(visibleApps, selectedPackage);
        empty.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
        grid.setVisibility(visibleApps.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
        focusSelected();
    }

    private void focusSelected() {
        if (selectedPackage == null) return;
        int position = -1;
        for (int index = 0; index < visibleApps.size(); index++) {
            if (selectedPackage.equals(visibleApps.get(index).packageName)) {
                position = index;
                break;
            }
        }
        if (position < 0) return;
        int target = position;
        grid.post(() -> {
            grid.scrollToPosition(target);
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(target);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private boolean isOnHome(AppLibraryCatalog.Entry entry) {
        return homePackages.contains(entry.packageName);
    }

    private void changeHome(AppLibraryCatalog.Entry entry, boolean onHome) {
        if (!LibraryApps.save(this, entry.packageName, onHome)) {
            Toast.makeText(this, R.string.library_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        HashSet<String> updatedHome = AppLibraryCatalog.withHomeState(homePackages, entry.packageName, onHome);
        homePackages.clear();
        homePackages.addAll(updatedHome);
        homeChanged = true;
        setResult(RESULT_OK);
        selectedPackage = entry.packageName;
        dismissActions();
        refreshCatalog();
    }

    private void open(AppLibraryCatalog.Entry entry) {
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(
                entry.resolveInfo.activityInfo.packageName, entry.resolveInfo.activityInfo.name));
        if (entry.leanback) intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, getString(R.string.cant_open, entry.label), Toast.LENGTH_SHORT).show();
        }
    }

    private void showActions(AppLibraryCatalog.Entry entry, View anchor) {
        dismissActions();
        View panel = getLayoutInflater().inflate(R.layout.popup_inputs, grid, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        addAction(rows, R.string.library_open, () -> open(entry));
        addAction(rows, isOnHome(entry) ? R.string.library_remove_shortcut : R.string.library_add_home,
                () -> changeHome(entry, !isOnHome(entry)));
        addAction(rows, R.string.library_app_info, () -> {
            dismissActions();
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + entry.packageName)));
            } catch (ActivityNotFoundException | SecurityException exception) {
                Toast.makeText(this, getString(R.string.cant_open, entry.label), Toast.LENGTH_SHORT).show();
            }
        });
        actionPopup = new PopupWindow(panel, dp(250), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        actionPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        actionPopup.setOutsideTouchable(true);
        actionPopup.setOnDismissListener(() -> actionPopup = null);
        actionPopup.showAsDropDown(anchor, 0, -anchor.getHeight(), Gravity.END);
        rows.getChildAt(0).requestFocus();
    }

    private void addAction(LinearLayout rows, int label, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((TextView) row.findViewById(R.id.title)).setText(label);
        row.findViewById(R.id.icon).setVisibility(View.GONE);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(view -> action.run());
        rows.addView(row);
    }

    private void dismissActions() {
        PopupWindow popup = actionPopup;
        actionPopup = null;
        if (popup != null) popup.dismiss();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class CatalogAdapter extends RecyclerView.Adapter<CatalogHolder> {
        @NonNull @Override public CatalogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_app_library, parent, false);
            return new CatalogHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull CatalogHolder holder, int position) {
            AppLibraryCatalog.Entry entry = visibleApps.get(position);
            holder.icon.setImageDrawable(entry.resolveInfo.loadIcon(getPackageManager()));
            holder.title.setText(entry.label);
            boolean onHome = isOnHome(entry);
            boolean recent = LibraryApps.recent(entry.installedAt, catalogTime);
            holder.status.setText(onHome ? R.string.library_on_home
                    : recent ? R.string.library_recent : R.string.library_add_home);
            holder.itemView.setOnClickListener(view -> {
                if (isOnHome(entry)) open(entry); else changeHome(entry, true);
            });
            holder.itemView.setOnLongClickListener(view -> {
                showActions(entry, view);
                return true;
            });
            holder.itemView.setOnFocusChangeListener((view, focused) -> {
                if (focused) selectedPackage = entry.packageName;
            });
            holder.itemView.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                return keyCode == KeyEvent.KEYCODE_DPAD_LEFT && holder.getBindingAdapterPosition() % 6 == 0;
            });
        }

        @Override public int getItemCount() { return visibleApps.size(); }
    }

    private static final class CatalogHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView status;

        CatalogHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            status = view.findViewById(R.id.status);
        }
    }
}
