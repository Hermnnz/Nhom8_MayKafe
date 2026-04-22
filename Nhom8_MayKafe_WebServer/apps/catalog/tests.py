from django.contrib.auth import get_user_model
from django.urls import reverse
from django.utils import timezone
from rest_framework.test import APITestCase

from apps.accounts.models import Profile
from apps.catalog.models import Category, Product
from apps.sales.models import Order, OrderItem


class ProductApiTests(APITestCase):
    def setUp(self):
        user_model = get_user_model()
        self.user = user_model.objects.create_user(username="admin_test", password="123456")
        self.user.profile.display_name = "Admin Test"
        self.user.profile.role = Profile.ROLE_ADMIN
        self.user.profile.save(update_fields=["display_name", "role"])
        self.client.force_authenticate(self.user)
        self.category = Category.objects.create(name="Tra & Tra sua")

    def test_create_product_without_asset_label_still_succeeds(self):
        response = self.client.post(
            reverse("product-list"),
            data={
                "name": "Ca phe sua",
                "price": 30000,
                "categoryId": self.category.id,
                "available": True,
                "imageUrl": "",
            },
            format="json",
        )

        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data["success"])
        product = Product.objects.get(name="Ca phe sua")
        self.assertEqual(product.category_id, self.category.id)
        self.assertIsNone(product.image)

    def test_delete_product_without_orders_hard_deletes_product(self):
        product = Product.objects.create(
            name="Banh mi",
            price=25000,
            available=True,
            category=self.category,
        )

        response = self.client.delete(reverse("product-detail", kwargs={"pk": product.pk}))

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data["success"])
        self.assertFalse(Product.objects.filter(pk=product.pk).exists())

    def test_delete_product_with_orders_returns_business_message(self):
        product = Product.objects.create(
            name="Tra dao",
            price=35000,
            available=True,
            category=self.category,
        )
        order = Order.objects.create(
            paid_at=timezone.now(),
            subtotal=35000,
            discount_amount=0,
            total_amount=35000,
            payment_method=Order.PAYMENT_CASH,
            status=Order.STATUS_PAID,
            cash_received=35000,
            change_amount=0,
            table_number=0,
        )
        OrderItem.objects.create(
            order=order,
            product=product,
            unit_price=35000,
            quantity=1,
            line_total=35000,
        )

        response = self.client.delete(reverse("product-detail", kwargs={"pk": product.pk}))

        self.assertEqual(response.status_code, 400)
        self.assertFalse(response.data["success"])
        self.assertEqual(response.data["message"], "Hiện không thể xóa món này")
        self.assertTrue(Product.objects.filter(pk=product.pk).exists())
