from django.urls import path
from rest_framework.routers import DefaultRouter

from apps.sales.views import (
    CashPaymentCartConfirmAPIView,
    CheckoutAPIView,
    OrderCashPaymentConfirmAPIView,
    OrderQrPaymentAPIView,
    OrderViewSet,
    PaymentConfirmAPIView,
    PaymentStatusAPIView,
    QrPaymentInitAPIView,
)


router = DefaultRouter()
router.register("", OrderViewSet, basename="order")

urlpatterns = [
    path("checkout/", CheckoutAPIView.as_view(), name="order-checkout"),
    path("payment/qr/", QrPaymentInitAPIView.as_view(), name="order-payment-qr-init"),
    path("payment/cash/confirm/", CashPaymentCartConfirmAPIView.as_view(), name="order-payment-cash-confirm"),
    path("<int:order_id>/payment/qr/", OrderQrPaymentAPIView.as_view(), name="order-payment-qr"),
    path("<int:order_id>/payment/cash/confirm/", OrderCashPaymentConfirmAPIView.as_view(), name="order-cash-confirm"),
    path("payments/<int:payment_id>/confirm/", PaymentConfirmAPIView.as_view(), name="payment-confirm"),
    path("payments/<int:payment_id>/status/", PaymentStatusAPIView.as_view(), name="payment-status"),
]

urlpatterns += router.urls
