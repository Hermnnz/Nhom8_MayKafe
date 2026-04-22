from django.db import migrations


class Migration(migrations.Migration):
    dependencies = [
        ("catalog", "0003_alter_product_image"),
    ]

    operations = [
        migrations.RemoveField(
            model_name="product",
            name="asset_label",
        ),
        migrations.RemoveField(
            model_name="product",
            name="accent_color_hex",
        ),
    ]
