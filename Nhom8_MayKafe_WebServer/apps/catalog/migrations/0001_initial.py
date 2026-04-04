from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True

    dependencies = []

    operations = [
        migrations.CreateModel(
            name="Category",
            fields=[
                ("id", models.AutoField(db_column="ID_DanhMuc", primary_key=True, serialize=False)),
                ("name", models.CharField(db_column="TenDanhMuc", max_length=50)),
            ],
            options={
                "db_table": "maycafe_danhmuc",
                "ordering": ["id"],
                "verbose_name": "Category",
                "verbose_name_plural": "Categories",
            },
        ),
        migrations.CreateModel(
            name="Product",
            fields=[
                ("id", models.AutoField(db_column="ID_Mon", primary_key=True, serialize=False)),
                ("name", models.CharField(db_column="TenMon", max_length=100)),
                ("price", models.FloatField(db_column="DonGia")),
                ("image", models.CharField(blank=True, db_column="HinhAnh", max_length=100, null=True)),
                ("available", models.BooleanField(db_column="TrangThai", default=True)),
                (
                    "category",
                    models.ForeignKey(
                        db_column="ID_DanhMuc_id",
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="products",
                        to="catalog.category",
                    ),
                ),
            ],
            options={
                "db_table": "maycafe_mon",
                "ordering": ["id"],
                "verbose_name": "Product",
                "verbose_name_plural": "Products",
            },
        ),
    ]
