from django.urls import path
from rest_framework.routers import DefaultRouter

from apps.sales.views import CheckoutAPIView, OrderViewSet


router = DefaultRouter()
router.register("", OrderViewSet, basename="order")

urlpatterns = [
    path("checkout/", CheckoutAPIView.as_view(), name="order-checkout"),
]

urlpatterns += router.urls
