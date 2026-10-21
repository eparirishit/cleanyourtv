package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private Config config;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ArrayList<String> packages = getIntent().getStringArrayListExtra(EXTRA_HOME_PACKAGES);
        if (packages != null) homePackages.addAll(packages);
        config = Config.load(this);
        setContentView(R.layout.activity_app_library);
        grid = findViewById(R.id.appLibraryGrid);
        filter = findViewById(R.id.appLibraryFilter);
        empty = findViewById(R.id.appLibraryEmpty);
        adapter = new CatalogAdapter();
        grid.setLayoutManager(new GridLayoutManager(this, AppLibraryLayout.columns()));
        grid.setItemAnimator(null);
        grid.setAdapter(adapter);
        grid.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                grid.post(() -> adapter.setGridWidth(grid.getWidth()));
            }
        });
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
        applyFilter();
    }

    private void applyFilter() {
        visibleApps.clear();
        visibleApps.addAll(AppLibraryCatalog.filter(allApps, filter == null ? "" : filter.getText().toString()));
        selectedPackage = AppLibraryCatalog.selectionAfterFilter(visibleApps, selectedPackage);
        empty.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
        grid.setVisibility(visibleApps.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
        if (AppLibraryPresentation.shouldFocusGrid(filter != null && filter.hasFocus())) focusSelected();
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
            if (holder != null) holder.itemView.findViewById(R.id.card).requestFocus();
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

    private Drawable banner(AppLibraryCatalog.Entry entry) {
        String configured = config.appBanners.get(entry.packageName);
        if (configured != null && configured.startsWith("banner_")) {
            int resourceId = getResources().getIdentifier(configured, "drawable", getPackageName());
            if (resourceId != 0) return getDrawable(resourceId);
        }
        PackageManager packageManager = getPackageManager();
        int resourceId = entry.resolveInfo.activityInfo.banner;
        if (resourceId == 0) resourceId = entry.resolveInfo.activityInfo.applicationInfo.banner;
        Drawable banner = null;
        if (resourceId != 0) {
            try {
                Resources resources = packageManager.getResourcesForApplication(
                        entry.resolveInfo.activityInfo.applicationInfo);
                int density = Math.max(android.util.DisplayMetrics.DENSITY_XHIGH,
                        getResources().getDisplayMetrics().densityDpi);
                banner = resources.getDrawableForDensity(resourceId, density, null);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
            }
        }
        if (banner == null) banner = entry.resolveInfo.activityInfo.loadBanner(packageManager);
        return banner != null && AppAdapter.isBannerShaped(banner.getIntrinsicWidth(), banner.getIntrinsicHeight())
                ? banner : null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class CatalogAdapter extends RecyclerView.Adapter<CatalogHolder> {
        private AppLibraryLayout.Geometry geometry;

        void setGridWidth(int width) {
            if (width <= 0) return;
            AppLibraryLayout.Geometry updated = AppLibraryLayout.geometry(width, AppLibraryLayout.columns(),
                    getResources().getDisplayMetrics().density);
            if (geometry != null && geometry.tileWidthPx == updated.tileWidthPx) return;
            geometry = updated;
            notifyDataSetChanged();
        }

        @NonNull @Override public CatalogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_app_library, parent, false);
            return new CatalogHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull CatalogHolder holder, int position) {
            applyGeometry(holder);
            boolean cardFocused = holder.card.isFocused();
            float scale = AppLibraryPresentation.tileScale(cardFocused);
            holder.card.setScaleX(scale);
            holder.card.setScaleY(scale);
            holder.card.setTranslationZ(cardFocused ? dp(6) : 0);
            AppLibraryCatalog.Entry entry = visibleApps.get(position);
            Drawable banner = banner(entry);
            if (banner != null) {
                holder.banner.setImageDrawable(banner);
                holder.banner.setVisibility(View.VISIBLE);
                holder.icon.setVisibility(View.GONE);
                holder.label.setVisibility(View.GONE);
            } else {
                holder.banner.setImageDrawable(null);
                holder.banner.setVisibility(View.GONE);
                holder.icon.setImageDrawable(entry.resolveInfo.loadIcon(getPackageManager()));
                holder.icon.setVisibility(View.VISIBLE);
                holder.label.setText(entry.label);
                holder.label.setVisibility(View.VISIBLE);
            }
            holder.name.setText(entry.label);
            boolean onHome = isOnHome(entry);
            holder.homeBadge.setVisibility(AppLibraryPresentation.showHomeBadge(onHome)
                    ? View.VISIBLE : View.GONE);
            holder.status.setVisibility(AppLibraryPresentation.showStatus() ? View.VISIBLE : View.GONE);
            holder.card.setOnClickListener(view -> {
                if (isOnHome(entry)) open(entry); else changeHome(entry, true);
            });
            holder.card.setOnLongClickListener(view -> {
                showActions(entry, view);
                return true;
            });
            holder.card.setOnFocusChangeListener((view, focused) -> {
                if (focused) selectedPackage = entry.packageName;
                float focusScale = AppLibraryPresentation.tileScale(focused);
                view.animate().cancel();
                view.setTranslationZ(focused ? dp(6) : 0);
                view.animate().scaleX(focusScale).scaleY(focusScale).setDuration(160).start();
            });
            holder.card.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        && holder.getBindingAdapterPosition() % AppLibraryLayout.columns() == 0;
            });
        }

        @Override public int getItemCount() { return visibleApps.size(); }

        private void applyGeometry(CatalogHolder holder) {
            if (geometry == null) {
                int width = grid.getWidth();
                if (width <= 0) return;
                geometry = AppLibraryLayout.geometry(width, AppLibraryLayout.columns(),
                        getResources().getDisplayMetrics().density);
            }
            ViewGroup.LayoutParams itemParams = holder.itemView.getLayoutParams();
            if (itemParams != null) {
                itemParams.height = geometry.itemHeightPx;
                holder.itemView.setLayoutParams(itemParams);
            }
            setWidthAndHeight(holder.card, geometry.tileWidthPx, geometry.tileHeightPx, 0);
            setWidthAndHeight(holder.name, geometry.tileWidthPx, 22, geometry.tileHeightPx + dp(7));
            setWidthAndHeight(holder.status, geometry.tileWidthPx, 18, geometry.tileHeightPx + dp(29));
            FrameLayout.LayoutParams homeBadgeParams = (FrameLayout.LayoutParams) holder.homeBadge.getLayoutParams();
            int homeBadgeSize = dp(AppLibraryPresentation.homeBadgeDiameterDp());
            homeBadgeParams.width = homeBadgeSize;
            homeBadgeParams.height = homeBadgeSize;
            holder.homeBadge.setLayoutParams(homeBadgeParams);
        }

        private void setWidthAndHeight(View view, int width, int heightDp, int topMargin) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            params.width = width;
            params.height = heightDp == geometry.tileHeightPx ? heightDp : dp(heightDp);
            params.topMargin = topMargin;
            view.setLayoutParams(params);
        }
    }

    private static final class CatalogHolder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView banner;
        final ImageView icon;
        final TextView label;
        final ImageView homeBadge;
        final TextView name;
        final TextView status;

        CatalogHolder(View view) {
            super(view);
            card = view.findViewById(R.id.card);
            float cornerRadius = 16 * view.getResources().getDisplayMetrics().density;
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View target, Outline outline) {
                    outline.setRoundRect(0, 0, target.getWidth(), target.getHeight(), cornerRadius);
                }
            });
            card.setClipToOutline(true);
            banner = view.findViewById(R.id.banner);
            icon = view.findViewById(R.id.icon);
            label = view.findViewById(R.id.label);
            homeBadge = view.findViewById(R.id.homeBadge);
            name = view.findViewById(R.id.name);
            status = view.findViewById(R.id.status);
        }
    }
}
