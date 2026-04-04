from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("catalog", "0002_product_visual_fields"),
    ]

    operations = [
        migrations.AlterField(
            model_name="product",
            name="image",
            field=models.CharField(blank=True, db_column="HinhAnh", max_length=500, null=True),
        ),
    ]
