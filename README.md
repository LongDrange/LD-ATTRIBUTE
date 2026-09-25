# LD-Attribute

自定義 RPG 屬性系統 · 卡片 · 魂珠 · 飾品 · 天賦 · 圖鑑 · 靈魂空間

適用 Minecraft 1.12.2 (Paper / Spigot)

---

## 功能模組

### 卡片系統
- 卡片背包（多頁）
- 卡片等級 / 星級 / 經驗石
- 卡片合成 / 分解 / 販賣
- 套裝 / 共鳴 / 羈絆 / Combo
- 卡片圖鑑 / 集齊獎勵
- 卡片抽獎 + 保底

### 魂珠空間（HZRing）
- Lore 自動識別（物品類型: 魂珠）
- 無限堆疊 · 多頁 · 分頁
- 槽位解鎖（點券 / 金幣 / 物品）
- 魂珠升級（屬性隨等級提升）
- 魂珠套裝
- 每種類型獨立上限

### 飾品背包
- 多頁 · 完全自訂義槽位
- 點擊背包飾品自動放入

### 天賦加點
- 多天賦頁 · 每頁獨立點數
- 前置天賦解鎖
- 一鍵連加（Shift+點擊）

### 怪物圖鑑
- 支援 MythicMobs 4.x / 5.x
- 擊殺累積 / 機率直接解鎖
- 解鎖石物品
- 永久屬性加成

### 靈魂空間（SoulRing）
- 無限堆疊 · 無限頁
- 4 種存入 / 4 種取出
- 自動拾取（怪物 / 挖礦）
- 分類切換 / 排序切換
- NBT 完整保留
- 掉落倍率（權限 / 限時 / 幸運）
- 靈魂兌換（配置驅動）

### 內建側邊欄 + Tab 頭尾
- 不依賴 TAB 插件

### 自訂義值（YeeValue）
- 任意數值 · 定時自動恢復

### 統一屬性來源註冊表
- 所有來源自動合併

### 存儲抽象層
- YAML / SQLite / MySQL 三選一

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
| /ldattribute | /lda | 主指令 |
| /ldring | /ring | 魂珠空間 |
| /ldsp | /jewelry | 飾品背包 |
| /ldtalent | /tl | 天賦加點 |
| /ldguide | /gd | 怪物圖鑑 |
| /ldvalue | /val | 自訂義值 |
| /ldsb | /sb | 側邊欄開關 |
| /ldsr | /sr | 靈魂空間 |
| /ldcore | - | 核心管理 |

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
| soulring.vip | 雙倍掉落 | - |
| soulring.svip | 三倍掉落 | - |

---

## 配置文件

所有配置文件位於 plugins/LD-Attribute/：

| 文件 | 說明 |
|---|---|
| config.yml | 主配置 |
| core.yml | 核心模組 |
| rings.yml | 魂珠空間 |
| ring-slots.yml | 魂珠槽位 |
| ring-sets.yml | 魂珠套裝 |
| ring-upgrade.yml | 魂珠升級 |
| jewelry.yml | 飾品背包 |
| talent.yml | 天賦 |
| guide.yml | 圖鑑 |
| value.yml | 自訂義值 |
| scoreboard.yml | 側邊欄 |
| soulring.yml | 靈魂空間 |
| rates.yml | 掉落倍率 |
| 配置/兌換商店/ | 靈魂兌換 |

---

## PlaceholderAPI 變數

### 魂珠空間
%ldring_total_<屬性>%
%ldring_count_<類型>%
%ldring_max_<類型>%
%ldring_limit_<類型>%
%ldring_has_<類型>%
%ldring_slots%
%ldring_pages%
%ldring_types%

text

### 自訂義值
%ldvalue_info_<值Id>%
%ldvalue_name_<值Id>%
%ldvalue_max_<值Id>%
%ldvalue_remain_<值Id>%
%ldvalue_has_<值Id>%

text

---

## 存儲方式

修改 core.yml：

```yaml
storage:
  type: YAML    # YAML / SQLITE / MYSQL
開發進度
☑ 核心框架
☑ 魂珠空間
☑ 飾品背包
☑ 天賦加點
☑ 統一屬性來源
☑ 怪物圖鑑
☑ 自訂義值
☑ PAPI 全場景
☑ 側邊欄 + Tab 頭尾
☑ 靈魂空間
☑ 靈魂自動拾取
☑ 靈魂倍率
☑ 靈魂兌換
□ 靈魂商店
□ SX-Attribute 直接對接
聯繫
作者: LongDrange

倉庫: https://github.com/LongDrange/LD-ATTRIBUTE

問題回報: https://github.com/LongDrange/LD-ATTRIBUTE/issues