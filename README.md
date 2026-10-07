# LazoBoombox

[English](#lazoboombox) · [Русский](#русский)

Take your music with you. LazoBoombox adds a portable boombox for [LazoDiscs](https://modrinth.com/mod/lazodiscs): hold it while walking or place it in the world. [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice) makes the music come from the player or the placed boombox, so nearby players can hear it too.

## Features

- Play music recorded with LazoDiscs while holding the boombox in either hand, or leave it playing as a placed block.
- Insert, remove, and swap recorded music discs.
- Dye the boombox with **1–8 dyes** in a crafting grid. Colors mix like leather armor, including the boombox's existing color when you dye it again.
- Color the casing, handle, and cassette panel together.
- Place the boombox in **16 directions**, with **22.5°** between each position.
- Keep its color and inserted disc when picking it up or breaking it. Creative pick-block also copies the color.
- See elapsed time and track duration while holding a playing boombox. You can move or hide this display in the client configuration.

## Crafting and controls

Craft a boombox with this recipe:

| Left | Center | Right |
| --- | --- | --- |
| Stone Button | Glass Pane | Stone Button |
| Iron Ingot | Jukebox | Iron Ingot |
| Gold Nugget | Redstone Dust | Gold Nugget |

Record a disc with `/lazodisc burn <url> [title]` first. Recording requires operator permissions by default; the server owner can change access in the LazoDiscs configuration.

**While holding the boombox:** put the disc in your other hand and use **Shift + Right Click** to insert or swap it. With your other hand empty, **Shift + Right Click** removes the stored disc. A recorded disc plays while you hold the boombox in either hand; put it away to stop handheld playback. **Right Click** on a block places the boombox.

**When the boombox is placed:** **Right Click** with a music disc to insert or swap it and start playback. **Right Click** with an empty hand removes the disc. **Shift + Right Click** picks up the boombox with its color and disc preserved.

**To dye it:** combine one boombox and 1–8 dyes in any arrangement in a crafting grid. You can add more dyes later to change its existing color; for example, adding white lightens a colored boombox. Dyeing keeps the stored disc.

## Installation

- **Required:** LazoDiscs and Plasmo Voice. Fabric builds also require [Fabric API](https://modrinth.com/mod/fabric-api).
- **Singleplayer:** install LazoBoombox and its dependencies in your Minecraft instance.
- **Multiplayer:** install LazoBoombox, LazoDiscs, and Plasmo Voice on **both the server and every player's client**. Fabric installations also need Fabric API.

Choose matching Minecraft versions and loaders for all dependencies. LazoDiscs is optional on clients when used on its own, but it is required on clients that use LazoBoombox.

## Configuration

- `config/lazoboombox/config.toml`: server settings for volume, hearing range, placed boomboxes, and the optional rule restricting Shift + Right Click pickup to the owner.
- `config/lazoboombox/client.toml`: show or hide the playback timer and change its screen position.

Experimental playback on Sable / Create Aeronautics moving platforms can be enabled in the server configuration. It is disabled by default and requires a compatible version of the platform mod.

## Supported builds

LazoBoombox **0.1.3** provides the following builds, based on the stable Plasmo Voice **2.1.17** compatibility list. Download the file for your exact Minecraft version and loader, together with the matching LazoDiscs build.

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.16.5, 1.17.1, 1.18.2 | ✓ | ✓ | — |
| 1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4 | ✓ | ✓ | — |
| 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4 | ✓ | ✓ | — |
| 1.21, 1.21.1 | ✓ | ✓ | ✓ |
| 1.21.4, 1.21.6, 1.21.7, 1.21.8, 1.21.11 | ✓ | — | ✓ |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | ✓ | — | ✓ |

[Modrinth](https://modrinth.com/mod/lazoboombox) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lazoboombox) · [GitHub releases](https://github.com/EyeCrasher07/LazoBoombox/releases) · [Report a bug](https://github.com/EyeCrasher07/LazoBoombox/issues)

---

## Русский

Возьми музыку с собой. LazoBoombox добавляет переносной бумбокс для LazoDiscs: его можно держать в руке во время прогулки или поставить в мире. Звук передаётся через Plasmo Voice и исходит от игрока или установленного бумбокса, поэтому музыку слышат и игроки поблизости.

### Возможности

- Воспроизведение записанной через LazoDiscs музыки из бумбокса в любой руке или установленного в мире.
- Установка, извлечение и замена записанных пластинок.
- Покраска **1–8 красителями** в сетке крафта. Цвета смешиваются, как у кожаной брони; при повторной покраске учитывается и уже нанесённый цвет.
- Вместе с корпусом красятся ручка и кассетная панель.
- Установка в **16 направлениях** с шагом **22,5°**.
- Сохранение цвета и пластинки при поднятии и разрушении. Копирование блока в творческом режиме также сохраняет цвет.
- Отображение прошедшего времени и длительности трека, когда играющий бумбокс в руке. Таймер можно переместить или скрыть в настройках клиента.

### Крафт и управление

Рецепт бумбокса:

| Слева | По центру | Справа |
| --- | --- | --- |
| Каменная кнопка | Стеклянная панель | Каменная кнопка |
| Железный слиток | Проигрыватель | Железный слиток |
| Кусочек золота | Редстоун | Кусочек золота |

Сначала запиши пластинку командой `/lazodisc burn <ссылка> [название]`. По умолчанию команды записи доступны операторам; владелец сервера может изменить доступ в конфиге LazoDiscs.

**Бумбокс в руке:** возьми пластинку в другую руку и нажми **Shift + ПКМ**, чтобы вставить или заменить её. Если другая рука пустая, **Shift + ПКМ** извлечёт пластинку. Записанная пластинка играет, пока бумбокс находится в любой руке; убери его из рук, чтобы остановить переносное воспроизведение. **ПКМ** по блоку ставит бумбокс в мире.

**Бумбокс установлен:** **ПКМ** с пластинкой вставляет или заменяет её и запускает музыку. **ПКМ** пустой рукой извлекает пластинку. **Shift + ПКМ** поднимает бумбокс, сохраняя его цвет и пластинку.

**Покраска:** положи один бумбокс и 1–8 красителей в сетку крафта в любом порядке. Его можно перекрашивать, добавляя красители к уже нанесённому цвету: например, белый сделает цвет светлее. Пластинка внутри сохраняется при покраске.

### Установка

- **Зависимости:** LazoDiscs и Plasmo Voice. Для Fabric также нужен Fabric API.
- **Одиночная игра:** установи LazoBoombox и зависимости в свой экземпляр Minecraft.
- **Мультиплеер:** установи LazoBoombox, LazoDiscs и Plasmo Voice **на сервер и клиент каждого игрока**. Для Fabric также нужен Fabric API.

Все зависимости должны соответствовать версии Minecraft и загрузчику. Сам по себе LazoDiscs необязателен на клиенте, но для работы LazoBoombox он нужен и игрокам.

### Настройки

- `config/lazoboombox/config.toml`: громкость, радиус слышимости, установленные бумбоксы и необязательное ограничение поднятия через Shift + ПКМ только владельцем.
- `config/lazoboombox/client.toml`: отображение таймера и его положение на экране.

Экспериментальное воспроизведение на движущихся платформах Sable / Create Aeronautics включается в конфиге сервера. По умолчанию оно отключено; для него нужна совместимая версия мода платформ.

### Поддерживаемые сборки

В LazoBoombox **0.1.3** доступны следующие сборки по списку совместимости стабильного Plasmo Voice **2.1.17**. Выбирай файл для своей точной версии Minecraft и загрузчика вместе с подходящей сборкой LazoDiscs.

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.16.5, 1.17.1, 1.18.2 | ✓ | ✓ | — |
| 1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4 | ✓ | ✓ | — |
| 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4 | ✓ | ✓ | — |
| 1.21, 1.21.1 | ✓ | ✓ | ✓ |
| 1.21.4, 1.21.6, 1.21.7, 1.21.8, 1.21.11 | ✓ | — | ✓ |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | ✓ | — | ✓ |

[Modrinth](https://modrinth.com/mod/lazoboombox) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lazoboombox) · [Релизы GitHub](https://github.com/EyeCrasher07/LazoBoombox/releases) · [Сообщить об ошибке](https://github.com/EyeCrasher07/LazoBoombox/issues)
