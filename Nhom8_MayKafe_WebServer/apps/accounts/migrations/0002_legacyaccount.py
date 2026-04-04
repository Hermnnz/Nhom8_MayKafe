from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="LegacyAccount",
            fields=[
                ("username", models.CharField(db_column="ID_TaiKhoan", max_length=150, primary_key=True, serialize=False)),
                ("password", models.CharField(db_column="MatKhau", max_length=255)),
                ("role_name", models.CharField(blank=True, db_column="ChucVu", max_length=50, null=True)),
            ],
            options={
                "verbose_name": "Legacy account",
                "verbose_name_plural": "Legacy accounts",
                "db_table": "maycafe_taikhoan",
                "ordering": ["username"],
                "managed": False,
            },
        ),
    ]
