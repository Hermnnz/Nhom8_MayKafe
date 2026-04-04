from django.contrib import admin

from apps.sales.models import Order, OrderItem


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("public_code", "paid_at", "status", "payment_method", "total_amount", "table_number")
    list_filter = ("status", "payment_method", "paid_at")
    search_fields = ("id",)
    inlines = (OrderItemInline,)


@admin.register(OrderItem)
class OrderItemAdmin(admin.ModelAdmin):
    list_display = ("id", "order", "product", "quantity", "unit_price", "line_total")
    list_filter = ("order__status",)
    search_fields = ("product__name", "order__id")
