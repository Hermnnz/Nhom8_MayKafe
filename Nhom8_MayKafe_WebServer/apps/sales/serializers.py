from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from apps.catalog.models import Product
from apps.catalog.services import build_asset_label, default_accent_color
from apps.sales.models import Order, OrderItem


class OrderItemSerializer(serializers.ModelSerializer):
    name = serializers.CharField(source="product.name", read_only=True)
    quantity = serializers.IntegerField(read_only=True)
    unitPrice = serializers.SerializerMethodField()
    note = serializers.SerializerMethodField()
    assetLabel = serializers.SerializerMethodField()
    accentColorHex = serializers.SerializerMethodField()

    class Meta:
        model = OrderItem
        fields = ("name", "quantity", "unitPrice", "note", "assetLabel", "accentColorHex")

    def get_unitPrice(self, obj):
        return int(obj.unit_price)

    def get_note(self, obj):
        return obj.note or ""

    def get_assetLabel(self, obj):
        return build_asset_label(obj.product.name)

    def get_accentColorHex(self, obj):
        return default_accent_color(getattr(obj.product.category, "name", None))


class OrderSerializer(serializers.ModelSerializer):
    pk = serializers.IntegerField(source="id", read_only=True)
    id = serializers.CharField(source="public_code", read_only=True)
    tableNumber = serializers.CharField(source="table_label", read_only=True)
    date = serializers.SerializerMethodField()
    time = serializers.SerializerMethodField()
    items = OrderItemSerializer(many=True, read_only=True)
    total = serializers.SerializerMethodField()
    status = serializers.CharField(read_only=True)
    paymentMethod = serializers.CharField(source="payment_method", read_only=True)
    paymentMethodLabel = serializers.CharField(source="payment_method_label", read_only=True)
    note = serializers.SerializerMethodField()
    subtotal = serializers.SerializerMethodField()
    discountAmount = serializers.SerializerMethodField()
    cashReceived = serializers.SerializerMethodField()
    changeAmount = serializers.SerializerMethodField()
    discountPercent = serializers.SerializerMethodField()

    class Meta:
        model = Order
        fields = (
            "pk",
            "id",
            "tableNumber",
            "date",
            "time",
            "items",
            "total",
            "status",
            "paymentMethod",
            "paymentMethodLabel",
            "note",
            "subtotal",
            "discountAmount",
            "discountPercent",
            "cashReceived",
            "changeAmount",
        )

    def get_date(self, obj):
        return timezone.localtime(obj.paid_at).strftime("%Y-%m-%d")

    def get_time(self, obj):
        return timezone.localtime(obj.paid_at).strftime("%H:%M")

    def get_total(self, obj):
        return int(obj.total_amount)

    def get_note(self, obj):
        return obj.note or ""

    def get_subtotal(self, obj):
        return int(obj.subtotal)

    def get_discountAmount(self, obj):
        return int(obj.discount_amount)

    def get_cashReceived(self, obj):
        return int(obj.cash_received)

    def get_changeAmount(self, obj):
        return int(obj.change_amount)

    def get_discountPercent(self, obj):
        if not obj.subtotal:
            return 0
        return round((obj.discount_amount / obj.subtotal) * 100)


class CheckoutItemInputSerializer(serializers.Serializer):
    productId = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1)
    note = serializers.CharField(required=False, allow_blank=True, allow_null=True)


class CheckoutSerializer(serializers.Serializer):
    tableNumber = serializers.CharField(required=False, allow_blank=True, allow_null=True, default="Bàn mới")
    discountPercent = serializers.IntegerField(min_value=0, max_value=100, default=0)
    paymentMethod = serializers.ChoiceField(choices=Order.PAYMENT_CHOICES)
    cashReceived = serializers.FloatField(required=False, min_value=0)
    items = CheckoutItemInputSerializer(many=True, allow_empty=False)

    def validate(self, attrs):
        payment_method = attrs["paymentMethod"]
        cash_received = attrs.get("cashReceived", 0)
        if payment_method == Order.PAYMENT_CASH and cash_received <= 0:
            raise serializers.ValidationError({"cashReceived": "Vui long nhap so tien khach dua."})
        product_ids = [item["productId"] for item in attrs["items"]]
        if len(product_ids) != len(set(product_ids)):
            raise serializers.ValidationError({"items": "Khong duoc gui trung san pham trong mot don hang."})
        return attrs

    def _parse_table_number(self, raw_value: str | None) -> int:
        if not raw_value:
            return 0
        digits = "".join(ch for ch in raw_value if ch.isdigit())
        return int(digits) if digits else 0

    @transaction.atomic
    def create(self, validated_data):
        discount_percent = validated_data["discountPercent"]
        payment_method = validated_data["paymentMethod"]
        items_data = validated_data["items"]

        product_map = {
            product.id: product
            for product in Product.objects.filter(id__in=[item["productId"] for item in items_data], available=True)
        }
        if len(product_map) != len(items_data):
            raise serializers.ValidationError({"items": "Mot hoac nhieu mon khong ton tai hoac da ngung ban."})

        subtotal = 0
        order_items_to_create = []
        for item in items_data:
            product = product_map[item["productId"]]
            line_total = product.price * item["quantity"]
            subtotal += line_total
            order_items_to_create.append(
                OrderItem(
                    product=product,
                    unit_price=product.price,
                    quantity=item["quantity"],
                    line_total=line_total,
                )
            )

        discount_amount = round(subtotal * discount_percent / 100)
        total_amount = max(0, subtotal - discount_amount)
        cash_received = validated_data.get("cashReceived", 0)
        if payment_method == Order.PAYMENT_CASH and cash_received < total_amount:
            raise serializers.ValidationError({"cashReceived": "So tien khach dua chua du."})

        order = Order.objects.create(
            paid_at=timezone.now(),
            subtotal=subtotal,
            discount_amount=discount_amount,
            total_amount=total_amount,
            payment_method=payment_method,
            status=Order.STATUS_PAID,
            cash_received=cash_received if payment_method == Order.PAYMENT_CASH else total_amount,
            change_amount=max(0, cash_received - total_amount) if payment_method == Order.PAYMENT_CASH else 0,
            table_number=self._parse_table_number(validated_data.get("tableNumber")),
        )

        for item in order_items_to_create:
            item.order = order
        OrderItem.objects.bulk_create(order_items_to_create)

        return order
