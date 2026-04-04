from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("catalog", "0001_initial"),
    ]

    operations = [
        migrations.AddField(
            model_name="product",
            name="asset_label",
            field=models.CharField(db_column="AssetLabel", default="MK", max_length=10),
        ),
        migrations.AddField(
            model_name="product",
            name="accent_color_hex",
            field=models.CharField(db_column="AccentColorHex", default="#6B3F2A", max_length=7),
        ),
    ]
