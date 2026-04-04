from datetime import timedelta

from django.contrib.auth.models import User
from django.core.management.base import BaseCommand
from django.db import transaction
from django.utils import timezone

from apps.catalog.models import Category, Product
from apps.sales.models import Order, OrderItem


DEMO_USERS = [
    {
        "username": "admin",
        "password": "123456",
        "display_name": "Nguyen Van An",
        "role": "ADMIN",
        "color": "#6B3F2A",
        "is_staff": True,
        "is_superuser": True,
    },
    {
        "username": "nhanvien",
        "password": "123456",
        "display_name": "Tran Thi Binh",
        "role": "STAFF",
        "color": "#C8956C",
        "is_staff": True,
        "is_superuser": False,
    },
]

DEMO_PRODUCTS = [
    ("Cà phê", "Cà phê đen", 25000, True, "CF", "#6B3F2A"),
    ("Cà phê", "Cà phê sữa", 30000, True, "CS", "#8B6355"),
    ("Cà phê", "Bạc xỉu", 32000, True, "BX", "#C8956C"),
    ("Cà phê", "Cappuccino", 45000, True, "CP", "#A15D3F"),
    ("Cà phê", "Latte", 48000, False, "LT", "#C08C58"),
    ("Trà", "Trà đào", 35000, True, "TD", "#E29A5C"),
    ("Trà", "Trà vải", 35000, True, "TV", "#D88F7D"),
    ("Trà", "Trà sữa", 38000, True, "TS", "#D0A36C"),
    ("Sinh tố", "Sinh tố xoài", 40000, True, "XM", "#F5A623"),
    ("Sinh tố", "Sinh tố bơ", 42000, True, "SB", "#80A866"),
    ("Nước ép", "Nước cam", 30000, True, "NC", "#F08B3A"),
    ("Bánh", "Bánh croissant", 28000, True, "CR", "#E0B56A"),
]


class Command(BaseCommand):
    help = "Seed demo users, catalog data and optional sample orders for local development."

    def add_arguments(self, parser):
        parser.add_argument(
            "--with-orders",
            action="store_true",
            help="Create sample paid and cancelled orders.",
        )

    @transaction.atomic
    def handle(self, *args, **options):
        self.seed_users()
        self.seed_catalog()
        if options["with_orders"]:
            self.seed_orders()
        self.stdout.write(self.style.SUCCESS("Demo data seeded successfully."))

    def seed_users(self):
        for row in DEMO_USERS:
            user, created = User.objects.get_or_create(
                username=row["username"],
                defaults={
                    "is_staff": row["is_staff"],
                    "is_superuser": row["is_superuser"],
                },
            )
            user.is_staff = row["is_staff"]
            user.is_superuser = row["is_superuser"]
            user.set_password(row["password"])
            user.save()

            profile = user.profile
            profile.display_name = row["display_name"]
            profile.role = row["role"]
            profile.avatar_color_hex = row["color"]
            profile.save()

            action = "Created" if created else "Updated"
            self.stdout.write(f"{action} user: {row['username']}")

    def seed_catalog(self):
        for category_name, name, price, available, asset_label, color in DEMO_PRODUCTS:
            category, _ = Category.objects.get_or_create(name=category_name)
            Product.objects.update_or_create(
                name=name,
                defaults={
                    "category": category,
                    "price": price,
                    "available": available,
                    "asset_label": asset_label,
                    "accent_color_hex": color,
                },
            )
        self.stdout.write(f"Seeded {len(DEMO_PRODUCTS)} products.")

    def seed_orders(self):
        if Order.objects.exists():
            self.stdout.write("Orders already exist. Skipped sample orders.")
            return

        products = {product.name: product for product in Product.objects.all()}
        sample_orders = [
            {
                "status": Order.STATUS_PAID,
                "payment_method": Order.PAYMENT_CASH,
                "table_number": 1,
                "items": [("Cà phê sữa", 2, ""), ("Bánh croissant", 1, "")],
                "discount_percent": 0,
                "hours_ago": 2,
                "note": "",
            },
            {
                "status": Order.STATUS_CANCELLED,
                "payment_method": Order.PAYMENT_QR,
                "table_number": 3,
                "items": [("Trà đào", 1, "Khách đổi món")],
                "discount_percent": 0,
                "hours_ago": 5,
                "note": "Khách hủy",
            },
        ]

        for payload in sample_orders:
            subtotal = sum(products[name].price * quantity for name, quantity, _ in payload["items"])
            discount_amount = round(subtotal * payload["discount_percent"] / 100)
            total_amount = subtotal - discount_amount
            order = Order.objects.create(
                paid_at=timezone.now() - timedelta(hours=payload["hours_ago"]),
                subtotal=subtotal,
                discount_amount=discount_amount,
                total_amount=total_amount,
                payment_method=payload["payment_method"],
                status=payload["status"],
                cash_received=total_amount if payload["payment_method"] == Order.PAYMENT_CASH else 0,
                change_amount=0,
                table_number=payload["table_number"],
                note=payload["note"],
            )
            for product_name, quantity, note in payload["items"]:
                product = products[product_name]
                OrderItem.objects.create(
                    order=order,
                    product=product,
                    unit_price=product.price,
                    quantity=quantity,
                    line_total=product.price * quantity,
                    note=note,
                )

        self.stdout.write("Seeded sample orders.")
