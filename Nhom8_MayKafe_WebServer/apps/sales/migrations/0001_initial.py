from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True

    dependencies = [
        ("catalog", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="Order",
            fields=[
                ("id", models.AutoField(db_column="ID_HD", primary_key=True, serialize=False)),
                ("paid_at", models.DateTimeField(db_column="NgayThanhToan")),
                ("subtotal", models.FloatField(db_column="TongTien", default=0)),
                ("discount_amount", models.FloatField(db_column="KhuyenMai", default=0)),
                ("total_amount", models.FloatField(db_column="TongThanhTien", default=0)),
                ("payment_method", models.CharField(choices=[("CASH", "Cash"), ("QR", "QR")], db_column="HinhThucThanhToan", max_length=50)),
                ("status", models.CharField(choices=[("PAID", "Paid"), ("PENDING", "Pending"), ("CANCELLED", "Cancelled")], db_column="TrangThai", default="PENDING", max_length=50)),
                ("cash_received", models.FloatField(db_column="TienKhachDua", default=0)),
                ("change_amount", models.FloatField(db_column="TienTraLai", default=0)),
                ("table_number", models.PositiveIntegerField(db_column="SoBan", default=0)),
            ],
            options={
                "db_table": "maycafe_banhang",
                "ordering": ["-paid_at", "-id"],
                "verbose_name": "Order",
                "verbose_name_plural": "Orders",
            },
        ),
        migrations.CreateModel(
            name="OrderItem",
            fields=[
                ("id", models.BigAutoField(primary_key=True, serialize=False)),
                ("unit_price", models.FloatField(db_column="DonGia")),
                ("quantity", models.PositiveIntegerField(db_column="SoLuong")),
                ("line_total", models.FloatField(db_column="ThanhTien")),
                ("note", models.CharField(blank=True, db_column="GhiChu", max_length=255, null=True)),
                (
                    "order",
                    models.ForeignKey(
                        db_column="ID_HD_id",
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="items",
                        to="sales.order",
                    ),
                ),
                (
                    "product",
                    models.ForeignKey(
                        db_column="ID_Mon_id",
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="order_items",
                        to="catalog.product",
                    ),
                ),
            ],
            options={
                "db_table": "maycafe_ct_banhang",
                "ordering": ["id"],
                "verbose_name": "Order item",
                "verbose_name_plural": "Order items",
            },
        ),
        migrations.AddConstraint(
            model_name="orderitem",
            constraint=models.UniqueConstraint(fields=("order", "product"), name="sales_order_product_unique"),
        ),
    ]
