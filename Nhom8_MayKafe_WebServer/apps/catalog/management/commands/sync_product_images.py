import re
import unicodedata
from collections import defaultdict
from pathlib import Path

from django.conf import settings
from django.core.management.base import BaseCommand, CommandError

from apps.catalog.models import Product

IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif"}


class Command(BaseCommand):
    help = "Dong bo truong HinhAnh cho mon tu thu muc media dua tren ten file anh."

    def add_arguments(self, parser):
        parser.add_argument(
            "--media-dir",
            default=str(settings.MEDIA_ROOT),
            help="Thu muc media can quet anh. Mac dinh la MEDIA_ROOT.",
        )
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Chi xem ket qua map anh ma khong ghi vao database.",
        )

    def handle(self, *args, **options):
        media_dir = Path(options["media_dir"]).resolve()
        dry_run = options["dry_run"]

        if not media_dir.exists() or not media_dir.is_dir():
            raise CommandError(f"Thu muc media khong ton tai: {media_dir}")

        image_map = self._build_image_map(media_dir)
        if not image_map:
            raise CommandError(f"Khong tim thay file anh nao trong {media_dir}")

        updated = []
        unchanged = []
        unmatched = []
        duplicates = []

        for product in Product.objects.select_related("category").order_by("id"):
            lookup_key = self._normalize_key(product.name)
            matches = image_map.get(lookup_key, [])
            if not matches:
                unmatched.append(product.name)
                continue

            chosen = self._pick_best_match(matches)
            relative_path = chosen.relative_to(media_dir).as_posix()
            if len(matches) > 1:
                duplicates.append((product.name, [match.name for match in matches], chosen.name))

            if product.image == relative_path:
                unchanged.append((product.name, relative_path))
                continue

            if not dry_run:
                Product.objects.filter(pk=product.pk).update(image=relative_path)
            updated.append((product.name, relative_path))

        self.stdout.write(self.style.SUCCESS(f"Da cap nhat {len(updated)} mon."))
        self.stdout.write(f"Giu nguyen {len(unchanged)} mon da co anh dung.")
        self.stdout.write(f"Chua map duoc {len(unmatched)} mon.")

        if updated:
            self.stdout.write("
Mon da duoc gan anh:")
            for name, path in updated:
                self.stdout.write(f"- {name} -> {path}")

        if duplicates:
            self.stdout.write("
Ten file trung key, da chon file dau theo thu tu sap xep:")
            for product_name, names, chosen in duplicates:
                self.stdout.write(f"- {product_name}: {', '.join(names)} | chon {chosen}")

        if unmatched:
            self.stdout.write("
Mon chua tim thay anh:")
            for name in unmatched:
                self.stdout.write(f"- {name}")

    def _build_image_map(self, media_dir: Path):
        image_map = defaultdict(list)
        for file_path in sorted(media_dir.rglob("*")):
            if not file_path.is_file() or file_path.suffix.lower() not in IMAGE_EXTENSIONS:
                continue
            key = self._normalize_key(file_path.stem)
            if not key:
                continue
            image_map[key].append(file_path)
        return image_map

    def _pick_best_match(self, matches):
        return sorted(matches, key=lambda item: (len(item.parts), len(item.name), item.name.casefold()))[0]

    def _normalize_key(self, value: str) -> str:
        normalized = unicodedata.normalize("NFKD", value or "")
        normalized = normalized.replace("?", "d").replace("?", "D")
        without_marks = "".join(ch for ch in normalized if not unicodedata.combining(ch))
        collapsed = re.sub(r"[^a-zA-Z0-9]+", " ", without_marks).strip().lower()
        return re.sub(r"\s+", " ", collapsed)
