# LD-Attribute

自定義 RPG 屬性系統 · 卡片 · 魂珠 · 飾品 · 天賦 · 圖鑑 · 靈魂空間 · 商店 · 符文

適用 Minecraft 1.12.2 (Paper / Spigot)

---

## 功能模組

### 📇 卡片系統
- 卡片背包（多頁 · 自訂義槽位）
- 卡片等級 / 星級 / 經驗石
- 卡片合成 / 分解 / 販賣
- 快捷分解（`/ldc decompose star/type/id`，只處理玩家主背包）
- 套裝 / 共鳴 / 羈絆 / Combo
- 卡片圖鑑 / 集齊獎勵
- 卡片抽獎 + 保底

### 💎 魂珠空間（HZRing）
- Lore 自動識別（物品類型: 魂珠）
- 無限堆疊 · 多頁 · 分頁
- 槽位解鎖（點券 / 金幣 / 物品）
- 魂珠升級（屬性隨等級提升）
- 魂珠套裝
- 每種類型獨立上限

### 💍 飾品背包
- 多頁 · 完全自訂義槽位
- 點擊背包飾品自動放入

### 🌟 天賦加點
- 多天賦頁 · 每頁獨立點數
- 前置天賦解鎖
- 一鍵連加（Shift+點擊）

### 📖 怪物圖鑑
- 支援 MythicMobs 4.x / 5.x
- 擊殺累積 / 機率直接解鎖
- 解鎖石物品
- 永久屬性加成

### 👻 靈魂空間（SoulRing）
- 無限堆疊 · 無限頁
- 4 種存入 / 4 種取出（左鍵 / 右鍵 / Shift+左鍵 / Shift+右鍵）
- **靈魂垃圾桶**（底部按鈕，批量刪除不要的物品）
- 自動拾取（怪物 / 挖礦）
- 分類切換 / 排序切換
- NBT 完整保留
- 掉落倍率（權限 / 限時 / 幸運）
- **靈魂兌換 / 商店**（配置驅動 · 支援 VAULT/POINT/VALUE 貨幣）

### 🔮 符文系統
- 5 種孔位（攻擊 / 防禦 / 通用 / 法術 / 特殊）
- 38 個符文 · 3 級升級
- 符文圖鑑 / 符文回收
- 卡片符文孔位顯示

### 🐾 寵物系統
- 11 種寵物 · 進化系統
- 寵物裝備（武器 / 護甲 / 飾品）
- 寵物等級 / 經驗

### 🏆 成就 & 抽獎
- 20 個成就 · 多類型觸發
- 3 個抽獎卡池 · 保底機制

### 🎮 自訂義值
- 任意數值 · 定時自動恢復
- 完整 PAPI 支援
- 統一存儲（YAML / SQLite / MySQL）

### 🔗 屬性對接
- **SX-Attribute** 對接（反射 + PAPI）
- **AttributePlus** 對接（反射 + PAPI）
- 統一屬性來源註冊表

### 📊 其他
- 內建側邊欄 + Tab 頭尾（不依賴 TAB 插件）
- 存儲抽象層（YAML / SQLite / MySQL 三選一）
- 操作日誌（`plugins/LD-Attribute/logs/admin.log`）

---

## 系統需求

| 項目 | 版本 |
|---|---|
| Minecraft | 1.12.2 |
| 服務端 | Paper / Spigot |
| Java | 8+ |
| PlaceholderAPI | 2.9.2+（可選） |
| Vault | 1.7.3+（可選） |
| MythicMobs | 4.4.0+（可選） |

---

## 指令

| 指令 | 別名 | 說明 |
|---|---|---|
| /ldc | /ldcard | 卡片主指令 |
| /ldattribute | /lda | 屬性主指令 |
| /ldring | /ring | 魂珠空間 |
| /ldsp | /jewelry | 飾品背包 |
| /ldtalent | /tl | 天賦加點 |
| /ldguide | /gd | 怪物圖鑑 |
| /ldvalue | /val / yeevalue | 自訂義值 |
| /ldsb | /sb | 側邊欄開關 |
| /ldsr | /sr | 靈魂空間 |
| /ldcore | /lcore | 核心管理 |

### 快捷分解

| 指令 | 說明 |
|---|---|
| `/ldc decompose star <N>` | 分解主背包中 ≤N 星的卡 |
| `/ldc decompose type <T1,T2>` | 分解指定類型（逗號分隔） |
| `/ldc decompose id <卡ID>` | 分解指定 ID |

> 註：快捷分解只處理**玩家主背包**，不會動卡片背包 / 靈魂空間。

---

## 權限

| 權限 | 說明 | 預設 |
|---|---|---|
| ldattribute.core.admin | 核心管理 | OP |
| ldattribute.ring.admin | 魂珠管理 | OP |
| ldattribute.jewelry.admin | 飾品管理 | OP |
| ldattribute.talent.admin | 天賦管理 | OP |
| ldattribute.guide.admin | 圖鑑管理 | OP |
| ldattribute.value.admin | 自訂義值管理 | OP |
| ldattribute.scoreboard.admin | 側邊欄管理 | OP |
| ldattribute.soulring.admin | 靈魂空間管理 | OP |
| **ldattribute.soulring.trash** | **靈魂垃圾桶使用權** | **OP** |
| **ldattribute.card.decompose** | **快捷分解權限** | **OP** |
| **ldattribute.exchange.shop** | **商店使用權** | **所有人** |
| soulring.vip | 雙倍掉落 | - |
| soulring.svip | 三倍掉落 | - |

---

## 配置文件

所有配置文件位於 plugins/LD-Attribute/：

| 文件 | 說明 |
|---|---|
| config.yml | 主配置 |
| core.yml | 核心模組 |
| value.yml | 自訂義值 |
| scoreboard.yml | 側邊欄 |
| soulring.yml | 靈魂空間 |
| rates.yml | 掉落倍率 |
| exchanges.yml | 靈魂兌換（舊版） |
| 配置/兌換商店/ | 商店（每頁一文件） |
| item.yml | 卡片定義 |
| cardlevel.yml | 卡片升級 |
| star.yml | 卡片升星 |
| decompose.yml | 卡片分解產出 |
| suit.yml / combo.yml / resonance.yml / bond.yml | 套裝 / 組合 / 共鳴 / 羈絆 |
| collection.yml | 圖鑑 |
| recipe.yml | 合成配方 |
| rings.yml / ring-slots.yml / ring-sets.yml / ring-upgrade.yml | 魂珠 |
| jewelry.yml | 飾品 |
| talent.yml | 天賦 |
| guide.yml | 圖鑑 |
| rune.yml | 符文 |
| spells.yml | 法術 |
| pet.yml / pet_equipment.yml | 寵物 |
| achievement.yml | 成就 |
| gacha.yml | 抽獎 |
| element.yml / state.yml / tempbuff.yml | 戰鬥 |
| mm.yml | MythicMobs 掉落 |
| ui.yml / stats_gui.yml / page.yml | 介面 |
| command.yml | 指令配置 |
| message/zh_TW.yml | 訊息文字 |
| lang/zh_TW.yml | 屬性別名 |

---

## PlaceholderAPI 變數

### 卡片
%ldattr_cards% 卡片數
%ldattr_points% 點券
%ldattr_pets% 寵物數
%ldattr_runes% 符文數
%ldattr_level_<ID>% 卡片等級
%ldattr_synergy_<ID>% 共鳴狀態
%ldattr_top_<屬性>_<名次>% 排行榜

text

### 魂珠空間
%ldring_total_<屬性>% 魂珠屬性總和
%ldring_count_<類型>% 某類型數量
%ldring_max_<類型>% 某類型上限
%ldring_limit_<類型>% 某類型限制
%ldring_has_<類型>% 是否有
%ldring_slots% 槽位數
%ldring_pages% 頁數
%ldring_types% 類型數

text

### 自訂義值
%ldvalue_info_<值Id>% 當前值
%ldvalue_name_<值Id>% 顯示名
%ldvalue_max_<值Id>% 最大值
%ldvalue_remain_<值Id>% 距離最大值還差多少
%ldvalue_has_<值Id>% 是否有此值

text

### 其他
%ldsb_...% 側邊欄
%ldsp_...% 飾品
%ldtalent_...% 天賦

text

---

## 存儲方式

修改 core.yml：

```yaml
storage:
  type: YAML    # YAML / SQLITE / MYSQL
開發進度
☑ 核心框架
☑ 卡片系統（含合成 / 分解 / 販賣 / 抽獎）
☑ 魂珠空間
☑ 飾品背包
☑ 天賦加點
☑ 怪物圖鑑
☑ 自訂義值
☑ PAPI 全場景
☑ 側邊欄 + Tab 頭尾
☑ 靈魂空間
☑ 靈魂自動拾取
☑ 靈魂倍率
☑ 靈魂兌換
☑ 靈魂垃圾桶（批量刪除）
☑ 靈魂商店（透過 Exchange 實現）
☑ 快捷分解
☑ SX-Attribute 對接
☑ AttributePlus 對接
☑ 符文系統
☑ 寵物系統
☑ 成就系統
☑ 操作日誌
聯繫
作者：LongDrange

倉庫：https://github.com/LongDrange/LD-ATTRIBUTE

問題回報：https://github.com/LongDrange/LD-ATTRIBUTE/issues

最新版本：v1.3.0+