import calendar
from collections import OrderedDict
from datetime import timedelta

from django.db.models import Count, Sum
from django.db.models.functions import ExtractMonth
from django.utils import timezone
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from apps.catalog.services import build_asset_label, default_accent_color, resolve_image_url
from apps.common.permissions import IsAdminRole
from apps.common.responses import success_response
from apps.sales.models import Order, OrderItem


class ReportDashboardAPIView(APIView):
    permission_classes = [IsAuthenticated, IsAdminRole]

    def get(self, request):
        period = request.query_params.get("period", "week").strip().lower()
        if period not in {"week", "month", "year"}:
            period = "week"

        current_range, previous_range = self._resolve_ranges(period)
        chart = self._build_chart(period, current_range)
        summary = self._build_summary(current_range, previous_range)
        top_items = self._build_top_items(current_range)
        category_shares = self._build_category_shares(current_range)

        return success_response(
            data={
                "period": period.upper(),
                "summary": summary,
                "chart": chart,
                "topItems": top_items,
                "categoryShares": category_shares,
            },
            message="Lay du lieu bao cao thanh cong.",
        )

    def _resolve_ranges(self, period: str):
        now = timezone.localtime()

        if period == "year":
            current_start = now.replace(month=1, day=1, hour=0, minute=0, second=0, microsecond=0)
            current_end = now
            previous_start = current_start.replace(year=current_start.year - 1)
            previous_end = current_end.replace(year=current_end.year - 1)
            return (current_start, current_end), (previous_start, previous_end)

        if period == "month":
            current_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
            current_end = now
            previous_month_end = current_start - timedelta(seconds=1)
            previous_start = previous_month_end.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
            previous_end = previous_month_end
            return (current_start, current_end), (previous_start, previous_end)

        weekday = now.weekday()
        current_start = (now - timedelta(days=weekday)).replace(hour=0, minute=0, second=0, microsecond=0)
        current_end = now
        previous_start = current_start - timedelta(days=7)
        previous_end = current_end - timedelta(days=7)
        return (current_start, current_end), (previous_start, previous_end)

    def _paid_orders_in_range(self, time_range):
        start, end = time_range
        return Order.objects.filter(status=Order.STATUS_PAID, paid_at__gte=start, paid_at__lte=end)

    def _build_summary(self, current_range, previous_range):
        current_orders = self._paid_orders_in_range(current_range)
        previous_orders = self._paid_orders_in_range(previous_range)

        current_revenue = current_orders.aggregate(total=Sum("total_amount"))["total"] or 0
        previous_revenue = previous_orders.aggregate(total=Sum("total_amount"))["total"] or 0
        current_order_count = current_orders.count()
        previous_order_count = previous_orders.count()
        current_average = current_revenue / current_order_count if current_order_count else 0
        previous_average = previous_revenue / previous_order_count if previous_order_count else 0

        current_customers = current_order_count
        previous_customers = previous_order_count

        revenue_change = self._percent_change(current_revenue, previous_revenue)
        orders_change = self._percent_change(current_order_count, previous_order_count)
        average_change = self._percent_change(current_average, previous_average)
        customers_change = self._percent_change(current_customers, previous_customers)

        return {
            "revenueChange": self._format_percent(revenue_change),
            "ordersChange": self._format_percent(orders_change),
            "averageChange": self._format_percent(average_change),
            "customersChange": self._format_percent(customers_change),
            "customers": current_customers,
            "revenueUp": revenue_change >= 0,
            "ordersUp": orders_change >= 0,
            "averageUp": average_change >= 0,
            "customersUp": customers_change >= 0,
        }

    def _build_chart(self, period: str, current_range):
        if period == "year":
            return self._build_year_chart(current_range)
        if period == "month":
            return self._build_month_chart(current_range)
        return self._build_week_chart(current_range)

    def _build_week_chart(self, current_range):
        start, _ = current_range
        orders = self._paid_orders_in_range(current_range)
        points = []
        weekday_labels = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]
        for index in range(7):
            day = start + timedelta(days=index)
            day_orders = orders.filter(paid_at__date=day.date())
            points.append(
                {
                    "label": weekday_labels[index],
                    "revenue": int(day_orders.aggregate(total=Sum("total_amount"))["total"] or 0),
                    "orders": day_orders.count(),
                }
            )
        return {
            "title": "Doanh thu theo ng\u00e0y",
            "metaLabel": "Tu\u1ea7n n\u00e0y",
            "points": points,
        }

    def _build_month_chart(self, current_range):
        start, _ = current_range
        orders = self._paid_orders_in_range(current_range)
        month_calendar = calendar.Calendar(firstweekday=0).monthdatescalendar(start.year, start.month)
        buckets = OrderedDict(
            (f"T{i}", {"label": f"T{i}", "revenue": 0, "orders": 0})
            for i in range(1, len(month_calendar) + 1)
        )
        week_lookup = {}
        for week_index, week_dates in enumerate(month_calendar, start=1):
            for week_date in week_dates:
                if week_date.month == start.month:
                    week_lookup[week_date] = week_index

        for order in orders:
            local_paid_at = timezone.localtime(order.paid_at)
            week_index = week_lookup.get(local_paid_at.date())
            if week_index is None:
                continue
            week_key = f"T{week_index}"
            buckets[week_key]["revenue"] += int(order.total_amount)
            buckets[week_key]["orders"] += 1

        return {
            "title": "Doanh thu theo tu\u1ea7n",
            "metaLabel": start.strftime("Th\u00e1ng %m"),
            "points": list(buckets.values()),
        }

    def _build_year_chart(self, current_range):
        orders = (
            self._paid_orders_in_range(current_range)
            .annotate(month=ExtractMonth("paid_at"))
            .values("month")
            .annotate(revenue=Sum("total_amount"), orders=Count("id"))
            .order_by("month")
        )
        order_map = {row["month"]: row for row in orders}
        points = []
        for month in range(1, 13):
            row = order_map.get(month)
            points.append(
                {
                    "label": f"T{month}",
                    "revenue": int((row or {}).get("revenue") or 0),
                    "orders": int((row or {}).get("orders") or 0),
                }
            )
        return {
            "title": "Doanh thu theo th\u00e1ng",
            "metaLabel": "N\u0103m nay",
            "points": points,
        }

    def _build_top_items(self, current_range):
        items = (
            OrderItem.objects.filter(
                order__status=Order.STATUS_PAID,
                order__paid_at__gte=current_range[0],
                order__paid_at__lte=current_range[1],
            )
            .values("product_id", "product__name", "product__category__name", "product__image")
            .annotate(
                count=Sum("quantity"),
                revenue=Sum("line_total"),
            )
            .order_by("-count", "-revenue")[:5]
        )

        return [
            {
                "name": item["product__name"],
                "count": int(item["count"] or 0),
                "revenue": int(item["revenue"] or 0),
                "assetLabel": build_asset_label(item["product__name"]),
                "accentColorHex": default_accent_color(item.get("product__category__name")),
                "imageUrl": resolve_image_url(item.get("product__image"), request=self.request),
            }
            for item in items
        ]

    def _build_category_shares(self, current_range):
        rows = (
            OrderItem.objects.filter(
                order__status=Order.STATUS_PAID,
                order__paid_at__gte=current_range[0],
                order__paid_at__lte=current_range[1],
            )
            .values("product__category__name")
            .annotate(revenue=Sum("line_total"))
            .order_by("-revenue")
        )
        total_revenue = sum(int(row["revenue"] or 0) for row in rows) or 1
        return [
            {
                "name": row["product__category__name"],
                "percent": round((int(row["revenue"] or 0) / total_revenue) * 100),
                "colorHex": default_accent_color(row.get("product__category__name")),
            }
            for row in rows
        ]

    def _percent_change(self, current_value, previous_value):
        if previous_value == 0:
            return 100.0 if current_value > 0 else 0.0
        return ((current_value - previous_value) / previous_value) * 100

    def _format_percent(self, value: float) -> str:
        prefix = "+" if value >= 0 else ""
        return f"{prefix}{value:.1f}%"
