from __future__ import annotations

from dataclasses import dataclass
from datetime import timedelta

from django.conf import settings
from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from apps.catalog.models import Product
from apps.sales.models import Order, OrderItem, OrderPayment


VIETQR_GUID = "A000000727"
VIETQR_SERVICE = "QRIBFTTA"


@dataclass
class PreparedOrderItem:
    product: Product
    quantity: int
    note: str
    unit_price: int
    line_total: int


@dataclass
class PreparedOrder:
    table_number: int
    subtotal: int
    discount_percent: int
    discount_amount: int
    total_amount: int
    items: list[PreparedOrderItem]


def parse_table_number(raw_value: str | None) -> int:
    if not raw_value:
        return 0
    digits = "".join(ch for ch in raw_value if ch.isdigit())
    return int(digits) if digits else 0


def prepare_order(table_number: str | None, discount_percent: int, items_data: list[dict]) -> PreparedOrder:
    product_ids = [item["productId"] for item in items_data]
    products = Product.objects.filter(id__in=product_ids, available=True).select_related("category")
    product_map = {product.id: product for product in products}
    if len(product_map) != len(items_data):
        raise serializers.ValidationError({"items": "Mot hoac nhieu mon khong ton tai hoac da ngung ban."})

    subtotal = 0
    prepared_items: list[PreparedOrderItem] = []
    for item in items_data:
        product = product_map[item["productId"]]
        quantity = int(item["quantity"])
        unit_price = int(round(product.price))
        line_total = unit_price * quantity
        subtotal += line_total
        prepared_items.append(
            PreparedOrderItem(
                product=product,
                quantity=quantity,
                note=(item.get("note") or "").strip(),
                unit_price=unit_price,
                line_total=line_total,
            )
        )

    discount_amount = round(subtotal * discount_percent / 100)
    total_amount = max(0, subtotal - discount_amount)
    return PreparedOrder(
        table_number=parse_table_number(table_number),
        subtotal=int(subtotal),
        discount_percent=int(discount_percent),
        discount_amount=int(discount_amount),
        total_amount=int(total_amount),
        items=prepared_items,
    )


@transaction.atomic
def create_order_from_prepared(prepared: PreparedOrder, payment_method: str, status: str) -> Order:
    order = Order.objects.create(
        paid_at=timezone.now(),
        subtotal=prepared.subtotal,
        discount_amount=prepared.discount_amount,
        total_amount=prepared.total_amount,
        payment_method=payment_method,
        status=status,
        cash_received=0,
        change_amount=0,
        table_number=prepared.table_number,
    )
    OrderItem.objects.bulk_create(
        [
            OrderItem(
                order=order,
                product=item.product,
                unit_price=item.unit_price,
                quantity=item.quantity,
                line_total=item.line_total,
                note=item.note or None,
            )
            for item in prepared.items
        ]
    )
    return order


def get_payment_receiver() -> dict[str, str]:
    return {
        "bank_name": settings.PAYMENT_RECEIVER_BANK_NAME,
        "bank_bin": settings.PAYMENT_RECEIVER_BANK_BIN,
        "account_number": settings.PAYMENT_RECEIVER_ACCOUNT_NUMBER,
        "account_name": settings.PAYMENT_RECEIVER_ACCOUNT_NAME,
    }


def build_transfer_content(order: Order) -> str:
    return settings.PAYMENT_TRANSFER_CONTENT


def _tlv(field_id: str, value: str) -> str:
    return f"{field_id}{len(value):02d}{value}"


def _crc16_ccitt(payload: str) -> str:
    crc = 0xFFFF
    for char in payload:
        crc ^= ord(char) << 8
        for _ in range(8):
            if crc & 0x8000:
                crc = ((crc << 1) ^ 0x1021) & 0xFFFF
            else:
                crc = (crc << 1) & 0xFFFF
    return f"{crc:04X}"


def build_vietqr_payload(amount: int, transfer_content: str) -> str:
    receiver = get_payment_receiver()
    beneficiary = _tlv("00", receiver["bank_bin"]) + _tlv("01", receiver["account_number"])
    merchant_account = (
        _tlv("00", VIETQR_GUID)
        + _tlv("01", beneficiary)
        + _tlv("02", VIETQR_SERVICE)
    )
    payload = (
        _tlv("00", "01")
        + _tlv("01", "12")
        + _tlv("38", merchant_account)
        + _tlv("53", "704")
        + _tlv("54", str(int(amount)))
        + _tlv("58", "VN")
        + _tlv("62", _tlv("08", transfer_content))
    )
    payload_with_crc = payload + "6304"
    return payload_with_crc + _crc16_ccitt(payload_with_crc)


def expire_payment_if_needed(payment: OrderPayment) -> OrderPayment:
    if (
        payment.status == OrderPayment.STATUS_PENDING
        and payment.expires_at
        and payment.expires_at <= timezone.now()
    ):
        payment.status = OrderPayment.STATUS_EXPIRED
        payment.save(update_fields=["status", "updated_at"])
    return payment


def expire_other_pending_payments(order: Order, exclude_payment_id: int | None = None) -> None:
    queryset = order.payments.filter(status=OrderPayment.STATUS_PENDING)
    if exclude_payment_id is not None:
        queryset = queryset.exclude(id=exclude_payment_id)
    queryset.update(status=OrderPayment.STATUS_FAILED, updated_at=timezone.now())


@transaction.atomic
def create_qr_order_payment(table_number: str | None, discount_percent: int, items_data: list[dict]) -> tuple[Order, OrderPayment]:
    prepared = prepare_order(table_number, discount_percent, items_data)
    order = create_order_from_prepared(prepared, payment_method=Order.PAYMENT_QR, status=Order.STATUS_PENDING)
    payment = create_or_refresh_qr_payment(order)
    return order, payment


@transaction.atomic
def create_pending_order(table_number: str | None, discount_percent: int, items_data: list[dict]) -> Order:
    prepared = prepare_order(table_number, discount_percent, items_data)
    return create_order_from_prepared(prepared, payment_method="", status=Order.STATUS_PENDING)


@transaction.atomic
def create_or_refresh_qr_payment(order: Order) -> OrderPayment:
    if order.status == Order.STATUS_PAID:
        raise serializers.ValidationError({"order": "Don hang nay da duoc thanh toan."})
    if order.status == Order.STATUS_CANCELLED:
        raise serializers.ValidationError({"order": "Don hang nay da bi huy."})

    latest_payment = order.payments.filter(method=OrderPayment.METHOD_BANK_TRANSFER).order_by("-id").first()
    if latest_payment is not None:
        latest_payment = expire_payment_if_needed(latest_payment)
        if latest_payment.status in {OrderPayment.STATUS_PENDING, OrderPayment.STATUS_WAITING_VERIFY}:
            return latest_payment

    receiver = get_payment_receiver()
    transfer_content = build_transfer_content(order)
    payment = OrderPayment.objects.create(
        order=order,
        method=OrderPayment.METHOD_BANK_TRANSFER,
        amount=int(round(order.total_amount)),
        bank_name=receiver["bank_name"],
        account_number=receiver["account_number"],
        account_name=receiver["account_name"],
        transfer_content=transfer_content,
        qr_content=build_vietqr_payload(int(round(order.total_amount)), transfer_content),
        status=OrderPayment.STATUS_PENDING,
        expires_at=timezone.now() + timedelta(minutes=settings.PAYMENT_QR_EXPIRES_MINUTES),
    )
    return payment


@transaction.atomic
def confirm_cash_payment(order: Order, cash_received: int) -> tuple[Order, OrderPayment]:
    if order.status == Order.STATUS_PAID:
        latest_payment = order.payments.filter(method=OrderPayment.METHOD_CASH, status=OrderPayment.STATUS_PAID).order_by("-id").first()
        if latest_payment is not None:
            return order, latest_payment
    if order.status == Order.STATUS_CANCELLED:
        raise serializers.ValidationError({"order": "Don hang nay da bi huy."})
    total_amount = int(round(order.total_amount))
    if cash_received < total_amount:
        raise serializers.ValidationError({"cashReceived": "So tien khach dua chua du."})

    now = timezone.now()
    expire_other_pending_payments(order)

    payment = OrderPayment.objects.create(
        order=order,
        method=OrderPayment.METHOD_CASH,
        amount=total_amount,
        status=OrderPayment.STATUS_PAID,
        paid_at=now,
        cash_received=cash_received,
        change_amount=max(0, cash_received - total_amount),
    )

    order.payment_method = Order.PAYMENT_CASH
    order.status = Order.STATUS_PAID
    order.cash_received = cash_received
    order.change_amount = max(0, cash_received - total_amount)
    order.paid_at = now
    order.save(
        update_fields=[
            "payment_method",
            "status",
            "cash_received",
            "change_amount",
            "paid_at",
        ]
    )
    return order, payment


@transaction.atomic
def create_cash_paid_order(table_number: str | None, discount_percent: int, cash_received: int, items_data: list[dict]) -> tuple[Order, OrderPayment]:
    prepared = prepare_order(table_number, discount_percent, items_data)
    order = create_order_from_prepared(prepared, payment_method=Order.PAYMENT_CASH, status=Order.STATUS_PENDING)
    return confirm_cash_payment(order, cash_received)


@transaction.atomic
def cancel_order(order: Order) -> Order:
    if order.status == Order.STATUS_PAID:
        raise serializers.ValidationError({"order": "Don hang nay da duoc thanh toan."})
    if order.status == Order.STATUS_CANCELLED:
        return order

    order.payments.filter(
        status__in=[OrderPayment.STATUS_PENDING, OrderPayment.STATUS_WAITING_VERIFY]
    ).update(status=OrderPayment.STATUS_FAILED, updated_at=timezone.now())

    order.status = Order.STATUS_CANCELLED
    order.paid_at = timezone.now()
    order.payment_method = ""
    order.cash_received = 0
    order.change_amount = 0
    order.save(update_fields=["status", "paid_at", "payment_method", "cash_received", "change_amount"])
    return order


@transaction.atomic
def confirm_bank_transfer(payment: OrderPayment) -> tuple[Order, OrderPayment]:
    if payment.status == OrderPayment.STATUS_PAID:
        return payment.order, payment
    payment = expire_payment_if_needed(payment)
    if payment.status == OrderPayment.STATUS_EXPIRED:
        raise serializers.ValidationError({"payment": "Ma thanh toan da het han."})
    if payment.order.status == Order.STATUS_CANCELLED:
        raise serializers.ValidationError({"order": "Don hang nay da bi huy."})

    now = timezone.now()
    payment.status = OrderPayment.STATUS_PAID
    payment.paid_at = now
    payment.save(update_fields=["status", "paid_at", "updated_at"])

    expire_other_pending_payments(payment.order, exclude_payment_id=payment.id)

    order = payment.order
    order.payment_method = Order.PAYMENT_QR
    order.status = Order.STATUS_PAID
    order.cash_received = 0
    order.change_amount = 0
    order.paid_at = now
    order.save(
        update_fields=[
            "payment_method",
            "status",
            "cash_received",
            "change_amount",
            "paid_at",
        ]
    )
    return order, payment
