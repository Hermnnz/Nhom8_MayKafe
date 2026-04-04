from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("sales", "0001_initial"),
    ]

    operations = [
        migrations.AddField(
            model_name="order",
            name="note",
            field=models.CharField(blank=True, db_column="GhiChu", max_length=255, null=True),
        ),
    ]
