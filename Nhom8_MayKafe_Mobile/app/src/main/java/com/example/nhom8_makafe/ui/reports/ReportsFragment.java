package com.example.nhom8_makafe.ui.reports;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.adapter.CategoryShareAdapter;
import com.example.nhom8_makafe.adapter.ReportTopItemAdapter;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.FragmentReportsBinding;
import com.example.nhom8_makafe.model.ReportChartSection;
import com.example.nhom8_makafe.model.ReportDashboardData;
import com.example.nhom8_makafe.model.ReportPeriod;
import com.example.nhom8_makafe.model.ReportPoint;
import com.example.nhom8_makafe.model.ReportSummary;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.List;

public class ReportsFragment extends Fragment {
    private FragmentReportsBinding binding;
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private ReportPeriod selectedPeriod = ReportPeriod.WEEK;
    private ReportTopItemAdapter topItemAdapter;
    private CategoryShareAdapter categoryShareAdapter;
    private int baseTopPadding;
    private int baseBottomPadding;

    public static ReportsFragment newInstance() {
        return new ReportsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseTopPadding = binding.layoutContent.getPaddingTop();
        baseBottomPadding = binding.layoutContent.getPaddingBottom();
        applyInsets();
        bindHeader();
        setupLists();
        setupPeriodControl();
        loadDashboard();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollReports, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.layoutContent.setPadding(
                    binding.layoutContent.getPaddingLeft(),
                    baseTopPadding + systemBars.top,
                    binding.layoutContent.getPaddingRight(),
                    baseBottomPadding + systemBars.bottom + dp(16)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.scrollReports);
    }

    private void bindHeader() {
        User user = sessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        binding.textAvatar.setText(user.getAvatarInitials());
        binding.textAvatar.setBackground(UiUtils.roundedBackground(user.getAvatarColorHex(), dp(22)));
        UiUtils.bindThumbnail(binding.textFooterAvatar, binding.textFooterAvatar, user.getAvatarInitials(), user.getAvatarColorHex());
        binding.textUserRole.setText(user.getDisplayName() + " \u00b7 " + UiUtils.roleLabel(user.getRole()));
        binding.textFooterName.setText(user.getDisplayName());
        binding.textFooterRole.setText(UiUtils.roleLabel(user.getRole()));
    }

    private void setupLists() {
        topItemAdapter = new ReportTopItemAdapter();
        binding.recyclerTopItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTopItems.setAdapter(topItemAdapter);

        categoryShareAdapter = new CategoryShareAdapter();
        binding.recyclerCategoryShares.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCategoryShares.setAdapter(categoryShareAdapter);
    }

    private void setupPeriodControl() {
        binding.buttonPeriodWeek.setOnClickListener(v -> setPeriod(ReportPeriod.WEEK));
        binding.buttonPeriodMonth.setOnClickListener(v -> setPeriod(ReportPeriod.MONTH));
        binding.buttonPeriodYear.setOnClickListener(v -> setPeriod(ReportPeriod.YEAR));
    }

    private void setPeriod(ReportPeriod period) {
        if (selectedPeriod == period) {
            return;
        }
        selectedPeriod = period;
        loadDashboard();
    }

    private void loadDashboard() {
        apiRepository.fetchReportDashboard(selectedPeriod, new ApiCallback<ReportDashboardData>() {
            @Override
            public void onSuccess(ReportDashboardData data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                render(data);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                topItemAdapter.submitList(java.util.Collections.emptyList());
                categoryShareAdapter.submitList(java.util.Collections.emptyList());
                binding.layoutChartBars.removeAllViews();
                updatePeriodButtons();
            }
        });
    }

    private void render(ReportDashboardData dashboardData) {
        ReportChartSection chartSection = dashboardData.getChartSection();
        ReportSummary summary = dashboardData.getSummary();
        List<ReportPoint> points = chartSection.getPoints();

        int totalRevenue = 0;
        int totalOrders = 0;
        int maxRevenue = 0;
        for (ReportPoint point : points) {
            totalRevenue += point.getRevenue();
            totalOrders += point.getOrders();
            maxRevenue = Math.max(maxRevenue, point.getRevenue());
        }
        int averageOrder = totalOrders == 0 ? 0 : Math.round((float) totalRevenue / totalOrders);

        bindKpi(binding.textValueRevenue, binding.textTrendRevenue,
                FormatUtils.formatCurrency(totalRevenue), summary.getRevenueChange(), summary.isRevenueUp());
        bindKpi(binding.textValueOrders, binding.textTrendOrders,
                String.valueOf(totalOrders), summary.getOrdersChange(), summary.isOrdersUp());
        bindKpi(binding.textValueAverage, binding.textTrendAverage,
                FormatUtils.formatCurrency(averageOrder), summary.getAverageChange(), summary.isAverageUp());
        bindKpi(binding.textValueCustomers, binding.textTrendCustomers,
                String.valueOf(summary.getCustomers()), summary.getCustomersChange(), summary.isCustomersUp());

        binding.textChartTitle.setText(chartSection.getTitle());
        binding.textChartSubtitle.setText(chartSection.getMetaLabel());
        binding.textFooterPeriod.setText(chartSection.getMetaLabel());
        binding.textFooterOrders.setText(totalOrders + " \u0111\u01a1n");

        applyChartHeight();
        final int finalMaxRevenue = maxRevenue;
        binding.layoutChartBars.post(() -> renderChart(points, finalMaxRevenue));

        topItemAdapter.submitList(dashboardData.getTopItems());
        categoryShareAdapter.submitList(dashboardData.getCategoryShares());
        updatePeriodButtons();
    }

    private void bindKpi(TextView valueView, TextView trendView, String value, String trend, boolean positive) {
        valueView.setText(value);
        trendView.setText((positive ? "\u2197 " : "\u2198 ") + trend);
        trendView.setTextColor(requireContext().getColor(positive ? R.color.success : R.color.danger));
    }

    private void applyChartHeight() {
        ViewGroup.LayoutParams params = binding.layoutChartBars.getLayoutParams();
        if (selectedPeriod == ReportPeriod.YEAR) {
            params.height = dp(126);
        } else if (selectedPeriod == ReportPeriod.MONTH) {
            params.height = dp(132);
        } else {
            params.height = dp(142);
        }
        binding.layoutChartBars.setLayoutParams(params);
    }

    private void renderChart(List<ReportPoint> points, int maxRevenue) {
        binding.layoutChartBars.removeAllViews();
        int gap = selectedPeriod == ReportPeriod.YEAR ? dp(3) : dp(4);
        int barWidth = getBarWidth();
        int minBarHeight = selectedPeriod == ReportPeriod.YEAR ? dp(46) : dp(54);
        int maxBarHeight = selectedPeriod == ReportPeriod.YEAR ? dp(98) : dp(110);

        for (int i = 0; i < points.size(); i++) {
            ReportPoint point = points.get(i);

            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) {
                columnParams.setMargins(gap, 0, 0, 0);
            }
            column.setLayoutParams(columnParams);

            Space spacer = new Space(requireContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            LinearLayout barHolder = new LinearLayout(requireContext());
            barHolder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            barHolder.setGravity(Gravity.CENTER_HORIZONTAL);

            View bar = new View(requireContext());
            int barHeight = Math.max(minBarHeight, Math.round(((float) point.getRevenue() / Math.max(maxRevenue, 1)) * maxBarHeight));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(barWidth, barHeight);
            bar.setLayoutParams(barParams);
            bar.setBackground(UiUtils.roundedBackground("#DFC0A3", dp(10)));
            barHolder.addView(bar);

            TextView label = new TextView(requireContext());
            label.setText(point.getLabel());
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setMaxLines(1);
            label.setIncludeFontPadding(false);
            label.setPadding(0, dp(7), 0, 0);
            label.setTextColor(requireContext().getColor(R.color.coffee_500));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedPeriod == ReportPeriod.YEAR ? 9.5f : 12f);

            column.addView(spacer);
            column.addView(barHolder);
            column.addView(label);
            binding.layoutChartBars.addView(column);
        }
    }

    private int getBarWidth() {
        if (selectedPeriod == ReportPeriod.YEAR) {
            return dp(20);
        }
        if (selectedPeriod == ReportPeriod.MONTH) {
            return dp(62);
        }
        return dp(40);
    }

    private void updatePeriodButtons() {
        bindPeriodButton(binding.buttonPeriodWeek, selectedPeriod == ReportPeriod.WEEK);
        bindPeriodButton(binding.buttonPeriodMonth, selectedPeriod == ReportPeriod.MONTH);
        bindPeriodButton(binding.buttonPeriodYear, selectedPeriod == ReportPeriod.YEAR);
    }

    private void bindPeriodButton(com.google.android.material.button.MaterialButton button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_report_segment_active : android.R.color.transparent);
        button.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.coffee_500));
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
