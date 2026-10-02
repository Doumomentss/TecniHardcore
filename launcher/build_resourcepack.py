import os
import json
from PIL import Image

appdata = os.environ.get("APPDATA")
pack_dir = os.path.join(appdata, ".minecraft", "resourcepacks", "TecniHardcore_Pack")
title_dir = os.path.join(pack_dir, "assets", "minecraft", "textures", "gui", "title")
bg_dir = os.path.join(title_dir, "background")

os.makedirs(bg_dir, exist_ok=True)

# 1. pack.mcmeta
mcmeta = {
    "pack": {
        "pack_format": 15,
        "description": "§c§lTecniHardcore §7- Menú y Pantalla de Carga Oficial"
    }
}
with open(os.path.join(pack_dir, "pack.mcmeta"), "w", encoding="utf-8") as f:
    json.dump(mcmeta, f, indent=2)

# Load base logo and bg
logo_path = r"d:\servers\minecraft\assets\tecnihardcore_logo.png"
bg_path = r"d:\servers\minecraft\assets\launcher_bg_epic.jpg"

logo_img = Image.open(logo_path).convert("RGBA")
bg_img = Image.open(bg_path).convert("RGBA")

# 2. pack.png (128x128)
pack_icon = logo_img.copy()
pack_icon.thumbnail((128, 128), Image.Resampling.LANCZOS)
icon_canvas = Image.new("RGBA", (128, 128), (10, 11, 18, 255))
offset_x = (128 - pack_icon.width) // 2
offset_y = (128 - pack_icon.height) // 2
icon_canvas.paste(pack_icon, (offset_x, offset_y), pack_icon)
icon_canvas.save(os.path.join(pack_dir, "pack.png"))

# 3. mojangstudios.png (512x512) - Replaces Mojang Studios loading splash!
mojang_canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
mojang_logo = logo_img.copy()
mojang_logo.thumbnail((480, 240), Image.Resampling.LANCZOS)
m_x = (512 - mojang_logo.width) // 2
m_y = (512 - mojang_logo.height) // 2
mojang_canvas.paste(mojang_logo, (m_x, m_y), mojang_logo)
mojang_canvas.save(os.path.join(title_dir, "mojangstudios.png"))

# 4. minecraft.png (1024x512) - In 1.20+, minecraft.png is 1024x512 (title on top half, reflection/shadows)
# Also in 1.20+, Minecraft uses minecraft.png or title.png
mc_canvas = Image.new("RGBA", (1024, 512), (0, 0, 0, 0))
mc_title = logo_img.copy()
mc_title.thumbnail((900, 240), Image.Resampling.LANCZOS)
t_x = (1024 - mc_title.width) // 2
t_y = 40
mc_canvas.paste(mc_title, (t_x, t_y), mc_title)
mc_canvas.save(os.path.join(title_dir, "minecraft.png"))

# 5. edition.png (transparent)
edition_canvas = Image.new("RGBA", (256, 64), (0, 0, 0, 0))
edition_canvas.save(os.path.join(title_dir, "edition.png"))

# 6. panorama_0.png to panorama_5.png (1024x1024)
bg_cropped = bg_img.resize((1024, 1024), Image.Resampling.LANCZOS)
for i in range(6):
    bg_cropped.save(os.path.join(bg_dir, f"panorama_{i}.png"))

print("ResourcePack TecniHardcore_Pack built successfully!")
