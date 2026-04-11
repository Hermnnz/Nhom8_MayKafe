from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("sales", "0002_order_note"),
    ]

    operations = [
        migrations.SeparateDatabaseAndState(
            database_operations=[],
            state_operations=[
                migrations.RemoveField(
                    model_name="order",
                    name="note",
                ),
            ],
        ),
        migrations.CreateModel(
            name="OrderPayment",
            fields=[
                ("id", models.BigAutoField(primary_key=True, serialize=False)),
                ("method", models.CharField(choices=[("cash", "Cash"), ("bank_transfer", "Bank transfer")], db_column="PhuongThuc", max_length=32)),
                ("amount", models.PositiveIntegerField(db_column="SoTien", default=0)),
                ("bank_name", models.CharField(blank=True, db_column="TenNganHang", default="", max_length=100)),
                ("account_number", models.CharField(blank=True, db_column="SoTaiKhoan", default="", max_length=50)),
                ("account_name", models.CharField(blank=True, db_column="TenTaiKhoan", default="", max_length=150)),
                ("transfer_content", models.CharField(blank=True, db_column="NoiDungChuyenKhoan", default="", max_length=120)),
                ("qr_content", models.TextField(blank=True, db_column="DuLieuQr", default="")),
                ("status", models.CharField(choices=[("pending", "Pending"), ("paid", "Paid"), ("failed", "Failed"), ("expired", "Expired"), ("waiting_verify", "Waiting verify")], db_column="TrangThaiThanhToan", default="pending", max_length=32)),
                ("expires_at", models.DateTimeField(blank=True, db_column="HetHanLuc", null=True)),
                ("paid_at", models.DateTimeField(blank=True, db_column="DaThanhToanLuc", null=True)),
                ("cash_received", models.FloatField(db_column="TienKhachDua", default=0)),
                ("change_amount", models.FloatField(db_column="TienTraLai", default=0)),
                ("note", models.CharField(blank=True, db_column="GhiChu", max_length=255, null=True)),
                ("raw_payload", models.TextField(blank=True, db_column="DuLieuGoc", default="")),
                ("created_at", models.DateTimeField(auto_now_add=True, db_column="NgayTao")),
                ("updated_at", models.DateTimeField(auto_now=True, db_column="NgayCapNhat")),
                (
                    "order",
                    models.ForeignKey(
                        db_column="ID_HD_id",
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="payments",
                        to="sales.order",
                    ),
                ),
            ],
            options={
                "db_table": "maycafe_thanhtoan",
                "ordering": ["-created_at", "-id"],
                "verbose_name": "Order payment",
                "verbose_name_plural": "Order payments",
            },
        ),
    ]
