from datetime import timedelta

from django.contrib.auth import get_user_model
from django.db import connection
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APIClient

from apps.catalog.models import Category, Product
from apps.sales.services import cancel_order, confirm_cash_payment, create_pending_order


class OrderDateBehaviorTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = get_user_model().objects.create_user(username="tester", password="secret123")
        self.client.force_authenticate(self.user)
        self.category = Category.objects.create(name="Ca phe")
        with connection.cursor() as cursor:
            cursor.execute(
                """
                INSERT INTO maycafe_mon
                    (TenMon, DonGia, HinhAnh, TrangThai, ID_DanhMuc_id, AssetLabel, AccentColorHex)
                VALUES
                    (%s, %s, %s, %s, %s, %s, %s)
                """,
                ["Ca phe sua", 30000, "", True, self.category.id, "CP", "#6B3F2A"],
            )
            product_id = cursor.lastrowid
        self.product = Product.objects.get(pk=product_id)

    def create_items_payload(self):
        return [
            {
                "productId": self.product.id,
                "quantity": 1,
            }
        ]

    def test_cancelled_order_uses_cancellation_date_in_summary_and_list(self):
        order = create_pending_order(None, 0, self.create_items_payload())
        order.paid_at = timezone.now() - timedelta(days=1)
        order.save(update_fields=["paid_at"])

        cancel_order(order)
        order.refresh_from_db()
        today = timezone.localdate(order.paid_at).isoformat()

        summary_response = self.client.get(reverse("order-summary"), {"date": today})
        self.assertEqual(summary_response.status_code, 200)
        self.assertEqual(summary_response.data["data"]["paidCount"], 0)
        self.assertEqual(summary_response.data["data"]["cancelledCount"], 1)
        self.assertEqual(summary_response.data["data"]["revenue"], 0)

        list_response = self.client.get(reverse("order-list"), {"date": today, "status": "CANCELLED"})
        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(list_response.data["meta"]["pagination"]["total_items"], 1)
        self.assertEqual(list_response.data["data"][0]["status"], "CANCELLED")

    def test_paid_order_keeps_payment_date_in_summary(self):
        order = create_pending_order(None, 0, self.create_items_payload())
        order.paid_at = timezone.now() - timedelta(days=1)
        order.save(update_fields=["paid_at"])

        confirm_cash_payment(order, 50000)
        order.refresh_from_db()
        today = timezone.localdate(order.paid_at).isoformat()

        summary_response = self.client.get(reverse("order-summary"), {"date": today})
        self.assertEqual(summary_response.status_code, 200)
        self.assertEqual(summary_response.data["data"]["paidCount"], 1)
        self.assertEqual(summary_response.data["data"]["cancelledCount"], 0)
        self.assertEqual(summary_response.data["data"]["revenue"], int(order.total_amount))

    def test_cancel_endpoint_allows_pending_order_created_from_checkout_flow(self):
        order = create_pending_order(None, 0, self.create_items_payload())

        response = self.client.post(reverse("order-cancel", kwargs={"pk": order.pk}))
        self.assertEqual(response.status_code, 200)

        order.refresh_from_db()
        self.assertEqual(order.status, "CANCELLED")
        self.assertEqual(response.data["data"]["status"], "CANCELLED")
