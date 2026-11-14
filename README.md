# EL Retrieve Slots

Addon for **Enigmatic Legacy** on **Minecraft 1.19.2 (Forge)**.
When you pick up the Extradimensional Vessel left by the Enigmatic Amulet after death, your items go back
to the slots they were in when you died, instead of being dumped into the inventory in random order.

*Русская версия — ниже.*

## Features
- Restores items to their original slots: hotbar, main inventory, armor, offhand.
- Supports **Curios** slots, including cosmetic Curios slots.
- Supports **Cosmetic Armor Reworked** slots (optional).
- If a slot is already taken (or the item no longer fits there), the item simply goes into the inventory as usual.
- The slot layout is saved inside the vessel itself, so it survives server restarts.
- Vessels created before the mod was installed keep working the old way.

## Installation
- **Server-side only.** Put the jar into the server's `mods` folder. Players do not need it and can join with or without it.
- In singleplayer, put it into your own `mods` folder.

## Requirements
| Mod | Version |
|---|---|
| Minecraft | 1.19.2 |
| Forge | 43.x |
| Enigmatic Legacy | 2.26+ |
| Curios API | 1.19.2-5.1+ |

## How it works
1. On death the mod records which slot held which stack.
2. When Enigmatic Legacy stores the drops in the Storage Crystal, the mod matches each stored stack to the
   recorded slots and writes that map into the crystal's NBT.
3. When the crystal is retrieved, every item Enigmatic Legacy would add to the inventory is put back into its
   old slot instead, if that slot is free.

## Building
Requires JDK 17.
```
./gradlew build
```
The jar ends up in `build/libs/`. Dependencies are downloaded from CurseForge through [CurseMaven](https://cursemaven.com).

## Credits
Idea inspired by [EL Addon Retrieve To Slot](https://www.curseforge.com/minecraft/mc-mods/el-addon-retrieve-to-slot)
by xewerdell (Minecraft 1.20.1). This mod is an independent implementation for 1.19.2 and does not contain any of its code.
Enigmatic Legacy is made by Aizistral.

## License
MIT — see [LICENSE](LICENSE).

---

# EL Retrieve Slots (RU)

Аддон для **Enigmatic Legacy** на **Minecraft 1.19.2 (Forge)**.
Когда подбираешь сосуд (Extradimensional Vessel), который оставляет загадочный амулет после смерти,
вещи возвращаются в те слоты, где лежали в момент смерти, а не высыпаются в инвентарь вперемешку.

## Возможности
- Возвращает вещи в их слоты: хотбар, инвентарь, броня, вторая рука.
- Поддерживает слоты **Curios**, включая косметические.
- Поддерживает слоты **Cosmetic Armor Reworked** (если мод установлен).
- Если слот уже занят (или вещь туда больше не подходит), она просто ложится в инвентарь, как обычно.
- Раскладка хранится внутри самого сосуда и переживает перезапуск сервера.
- Сосуды, созданные до установки мода, работают по-старому.

## Установка
- **Только на сервер.** Положите jar в папку `mods` сервера. Игрокам ставить не нужно, зайти можно и с модом, и без него.
- В одиночной игре — в свою папку `mods`.

## Требования
Minecraft 1.19.2, Forge 43.x, Enigmatic Legacy 2.26+, Curios API 1.19.2-5.1+.

## Благодарности
Идея взята у аддона [EL Addon Retrieve To Slot](https://www.curseforge.com/minecraft/mc-mods/el-addon-retrieve-to-slot)
от xewerdell (для 1.20.1). Этот мод написан с нуля под 1.19.2 и не содержит его кода.
Enigmatic Legacy — мод от Aizistral.
