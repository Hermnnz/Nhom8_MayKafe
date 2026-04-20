from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from apps.catalog.services import build_asset_label, default_accent_color
from apps.sales.models import Order, OrderItem, OrderPayment
from apps.sales.services import (
    cancel_order,
    confirm_bank_transfer,
    confirm_cash_payment,
    create_cash_paid_order,
    create_pending_order,
    create_or_refresh_qr_payment,
    create_qr_order_payment,
    expire_payment_if_needed,
)


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


class OrderPaymentSerializer(serializers.ModelSerializer):
    paymentId = serializers.IntegerField(source="id", read_only=True)
    orderId = serializers.IntegerField(source="order.id", read_only=True)
    orderCode = serializers.CharField(source="order.public_code", read_only=True)
    amount = serializers.SerializerMethodField()
    bankName = serializers.CharField(source="bank_name", read_only=True)
    accountNumber = serializers.CharField(source="account_number", read_only=True)
    accountName = serializers.CharField(source="account_name", read_only=True)
    transferContent = serializers.CharField(source="transfer_content", read_only=True)
    qrContent = serializers.CharField(source="qr_content", read_only=True)
    status = serializers.CharField(read_only=True)
    expiresAt = serializers.DateTimeField(source="expires_at", read_only=True)
    paidAt = serializers.DateTimeField(source="paid_at", read_only=True)

    class Meta:
        model = OrderPayment
        fields = (
            "paymentId",
            "orderId",
            "orderCode",
            "amount",
            "bankName",
            "accountNumber",
            "accountName",
            "transferContent",
            "qrContent",
            "status",
            "expiresAt",
            "paidAt",
        )

    def get_amount(self, obj):
        return int(obj.amount)


class CheckoutItemInputSerializer(serializers.Serializer):
    productId = serializers.IntegerField()
    quantity = serializers.IntegerField(min_value=1)
    note = serializers.CharField(required=False, allow_blank=True, allow_null=True)


class CartOrderBaseSerializer(serializers.Serializer):
    tableNumber = serializers.CharField(required=False, allow_blank=True, allow_null=True, default="Bàn mới")
    discountPercent = serializers.IntegerField(min_value=0, max_value=100, default=0)
    items = CheckoutItemInputSerializer(many=True, allow_empty=False)

    def validate(self, attrs):
        product_ids = [item["productId"] for item in attrs["items"]]
        if len(product_ids) != len(set(product_ids)):
            raise serializers.ValidationError({"items": "Khong duoc gui trung san pham trong mot don hang."})
        return attrs


class CheckoutSerializer(CartOrderBaseSerializer):
    paymentMethod = serializers.ChoiceField(choices=Order.PAYMENT_CHOICES)
    cashReceived = serializers.FloatField(required=False, min_value=0)

    def validate(self, attrs):
        attrs = super().validate(attrs)
        if attrs["paymentMethod"] == Order.PAYMENT_CASH and attrs.get("cashReceived", 0) <= 0:
            raise serializers.ValidationError({"cashReceived": "Vui long nhap so tien khach dua."})
        return attrs

    @transaction.atomic
    def create(self, validated_data):
        payment_method = validated_data["paymentMethod"]
        if payment_method == Order.PAYMENT_CASH:
            order, _payment = create_cash_paid_order(
                validated_data.get("tableNumber"),
                validated_data["discountPercent"],
                int(validated_data.get("cashReceived", 0)),
                validated_data["items"],
            )
            return order

        order, payment = create_qr_order_payment(
            validated_data.get("tableNumber"),
            validated_data["discountPercent"],
            validated_data["items"],
        )
        order, _payment = confirm_bank_transfer(payment)
        return order


class QrPaymentInitSerializer(CartOrderBaseSerializer):
    @transaction.atomic
    def create(self, validated_data):
        _order, payment = create_qr_order_payment(
            validated_data.get("tableNumber"),
            validated_data["discountPercent"],
            validated_data["items"],
        )
        return payment


class PendingOrderCreateSerializer(CartOrderBaseSerializer):
    @transaction.atomic
    def create(self, validated_data):
        return create_pending_order(
            validated_data.get("tableNumber"),
            validated_data["discountPercent"],
            validated_data["items"],
        )


class CashPaymentCartConfirmSerializer(CartOrderBaseSerializer):
    cashReceived = serializers.FloatField(min_value=0)

    def validate(self, attrs):
        attrs = super().validate(attrs)
        if attrs["cashReceived"] <= 0:
            raise serializers.ValidationError({"cashReceived": "Vui long nhap so tien khach dua."})
        return attrs

    @transaction.atomic
    def create(self, validated_data):
        order, _payment = create_cash_paid_order(
            validated_data.get("tableNumber"),
            validated_data["discountPercent"],
            int(validated_data["cashReceived"]),
            validated_data["items"],
        )
        return order


class CashPaymentConfirmSerializer(serializers.Serializer):
    cashReceived = serializers.FloatField(min_value=0)

    def validate_cashReceived(self, value):
        if value <= 0:
            raise serializers.ValidationError("Vui long nhap so tien khach dua.")
        return value

    @transaction.atomic
    def save(self, **kwargs):
        order: Order = self.context["order"]
        cash_received = int(self.validated_data["cashReceived"])
        order, _payment = confirm_cash_payment(order, cash_received)
        return order


class PaymentConfirmSerializer(serializers.Serializer):
    def save(self, **kwargs):
        payment: OrderPayment = self.context["payment"]
        payment = expire_payment_if_needed(payment)
        order, _payment = confirm_bank_transfer(payment)
        return order


class PaymentStatusSerializer(serializers.Serializer):
    payment = OrderPaymentSerializer()

    def to_representation(self, instance):
        payment = expire_payment_if_needed(instance)
        return OrderPaymentSerializer(payment).data


class OrderQrPaymentSerializer(serializers.Serializer):
    def save(self, **kwargs):
        order: Order = self.context["order"]
        payment = create_or_refresh_qr_payment(order)
        return payment


class OrderCancelSerializer(serializers.Serializer):
    def save(self, **kwargs):
        order: Order = self.context["order"]
        return cancel_order(order)
