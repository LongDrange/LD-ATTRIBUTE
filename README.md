# LD-Attribute

自定義 RPG 屬性系統 · 卡片 · 魂珠 · 飾品 · 天賦 · 圖鑑 · 靈魂空間 · 商店 · 符文 · 遺物 · 掉落增強 · 殺戮

適用 Minecraft 1.12.2 (Paper / Spigot) · 當前版本 **v1.6.0**

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
- **卡片配置熱刷新**（`/ldc reload` 自動同步所有在線玩家的卡片 Lore/NBT）

### 💎 魂珠空間（HZRing）
- Lore 自動識別（物品類型: 魂珠）
- 無限堆疊 · 多頁 · 分頁
- 槽位解鎖（點券 / 金幣 / 物品）
- 魂珠升級（屬性隨等級提升，支援成功率 + 失敗處理 KEEP/LOSE/REFUND）
- 魂珠套裝
- 每種類型獨立上限
- **獨立魂珠類型配置**：`配置/魂珠/<分類>/*.yml`，支援多檔案 / 多資料夾
- **`/ldring give <玩家> <魂珠ID> [數量]`** 發放魂珠

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
- **收藏分系統**：每個圖鑑可設定固定分 / 稀有度分
- **套裝集齊加分**：集齊一整組圖鑑額外加分 + 獨立屬性
- **稱號系統**：累積收藏分解鎖稱號（永久屬性 + 聊天前綴 + 獲得命令）
- **GUI 顯示**：收藏分 / 當前稱號 / 下一檔位進度

### 👻 靈魂空間（SoulRing）
- 無限堆疊 · 無限頁
- 4 種存入 / 4 種取出（左鍵 / 右鍵 / Shift+左鍵 / Shift+右鍵）
- 自動拾取（怪物 / 挖礦）
- 分類切換 / 排序切換
- NBT 完整保留
- 掉落倍率（權限 / 限時 / 幸運）
- 靈魂兌換 / 商店（配置驅動 · 支援 VAULT/POINT/VALUE 貨幣）
- **批量分解 / 刪除模式**（底部按鈕切換儲存/分解/刪除）
- **LORE 黑名單 / 白名單**（包含 / 完全匹配 / 正則 + 白名單優先）
- **操作日誌**：`logs/soulring/<玩家名>.log`（每玩家獨立 + UUID 查詢）
- **`/ldsr log <玩家名或UUID> [行數]`** 查詢操作記錄

### 🔮 符文系統
- 5 種孔位（攻擊 / 防禦 / 通用 / 法術 / 特殊）
- 38 個符文 · 3 級升級
- 符文圖鑑 / 符文回收
- 卡片符文孔位顯示
- **鑲嵌成功率 + 失敗處理**（KEEP/LOSE/REFUND 三種，每個符文獨立配置）
- **鑲嵌消耗**：Vault + 點券 + 自定義值 都支援

### 🔥 遺物系統（全新）
- **動態槽位**：數量 / 位置 / 圖標 / 排序全可配置
- **主屬性隨機範圍** + **副屬性精細控制**
  - `SubCount`：副屬性條數
  - `SubPool`：自訂副屬性池（本地 / 全局）
  - `SubFixed`：固定副屬性（固定值 + 範圍）
- **套裝效果**：2 / 4 / 6 件自動觸發額外屬性
- **鎖定功能**：鎖定後防覆蓋 / 防誤卸
- **多檔案 / 多資料夾**：`配置/遺物/<分類>/*.yml`
- **GUI 操作**：左鍵裝備 / Shift+左鍵卸下 / 右鍵鎖定
- **`/ldrelic give/list/reload`** + Tab 補全

### 🎯 掉落增強（全新）
- **爆率屬性**：`最終機率 = 基礎機率 × (1 + 爆率/100)`，封頂 100%
- **LD 掉落表**：`配置/掉落/掉落表.yml`
  - 支援 `RING:` / `CARD:` / `RUNE:` / 材質 物品語法
  - 每條可配：機率 / 數量範圍 / 爆率開關 / 廣播 / 權限 / 每日限制
- **MythicMobs 掉落接管**：自動讀取 MM 的 Mobs + DropTables，爆率加成
- **多重掉落目標**：`killer` / `nearby:N` / `world` / `server`
- **全服廣播**：稀有物品掉落時全服通告
- **每日限制**：`dailyLimit` 限制某物品每天掉落次數

### ⚔️ 殺戮系統（全新）
- **獨立玩家開關**（持久化）
- **權限組配置**：VIP / SVIP 等，玩家有多組時取範圍最大
- **獨立玩家配置**：管理員用 `/laa set <玩家> <參數> <值>` 精細控制
- **大小寫不敏感**：`LongBing` = `longbing`，自動去重
- **可自訂**：攻擊範圍 / 間隔 / 同時目標數 / 防擊退 / 自動轉向 / 動畫
- **攻速反作弊**：
  - 統計玩家每秒攻擊次數
  - 超過上限 → 記違規
  - 累計 3 次警告 → 踢出 + 全服廣播
  - 殺戮觸發的攻擊自動豁免

### 🐾 寵物系統
- 11 種寵物 · 進化系統
- 寵物裝備（武器 / 護甲 / 飾品）
- 寵物等級 / 經驗

### 🏆 成就 & 抽獎
- 20 個成就 · 多類型觸發
- 3 個抽獎卡池 · 保底機制
- **抽獎支援 `ring:<魂珠ID>:<權重>`**

### 🎮 自訂義值
- 任意數值 · 定時自動恢復
- 完整 PAPI 支援
- 統一存儲（YAML / SQLite / MySQL）

### 🔗 屬性對接
- **SX-Attribute** 對接（反射 + PAPI）
- **AttributePlus** 對接（反射 + PAPI）
- 統一屬性來源註冊表
- **統一物品解析器 ItemResolver**
  - `RING:<ID>` / `CARD:<ID>` / `RUNE:<ID>` / 原版材質
  - 兌換商店 / 抽獎 / 掉落表 / 商店 全部共用

### 🈶 中文簡繁轉換（全新）
- `ChineseConverter` 工具類，覆蓋 1500+ 繁簡字對
- `toSimplified` / `toTraditional` / `normalize` / `equals` / `contains`
- 所有屬性匹配自動繁簡歸一化（`暴擊機率` = `暴击机率`）

### 🔢 變數縮寫（全新）
- `NumberAbbrev` 工具類
- 數字縮寫：`15000 → 1.5萬` / `1.5e9 → 15億`
- 可配置閾值（萬 / 億 / 兆）+ 小數位數
- PAPI 純數值佔位符自動縮寫

### 📊 其他
- 內建側邊欄 + Tab 頭尾（不依賴 TAB 插件）
- 存儲抽象層（YAML / SQLite / MySQL 三選一）
- 操作日誌（`plugins/LD-Attribute/logs/admin.log`）
- 自動備份（每 30 分鐘，保留 24 份）

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
| /ldvalue | /val | 自訂義值 |
| /ldsb | /sb | 側邊欄開關 |
| /ldsr | /sr | 靈魂空間 |
| /ldcore | /lcore | 核心管理 |
| **/ldrelic** | **/relic, /yw** | **遺物系統** |
| **/laa** | **/autoattack** | **殺戮系統** |

### 快捷分解

| 指令 | 說明 |
|---|---|
| `/ldc decompose star <N>` | 分解主背包中 ≤N 星的卡 |
| `/ldc decompose type <T1,T2>` | 分解指定類型（逗號分隔） |
| `/ldc decompose id <卡ID>` | 分解指定 ID |

> 註：快捷分解只處理**玩家主背包**，不會動卡片背包 / 靈魂空間。

### 遺物系統

| 指令 | 說明 |
|---|---|
| `/ldrelic` | 打開遺物界面 |
| `/ldrelic list` | 列出所有遺物ID |
| `/ldrelic give <玩家> <ID> [數量]` | 給遺物（管理員） |
| `/ldrelic reload` | 重載配置（管理員） |

### 殺戮系統

| 指令 | 說明 |
|---|---|
| `/laa` | 切換開關 |
| `/laa on\|off` | 強制開關 |
| `/laa info` | 查看當前配置 |
| `/laa set <玩家> <參數> <值>` | 獨立配置（管理員） |
| `/laa clear <玩家>` | 清除獨立配置（管理員） |
| `/laa list` | 列出獨立配置玩家（管理員） |
| `/laa reload` | 重載配置（管理員） |

**`/laa set` 支援的參數**：`range` / `interval` / `targets` / `antiknockback` / `autorotate`

### 靈魂空間

| 指令 | 說明 |
|---|---|
| `/ldsr log <玩家名或UUID> [行數]` | 查詢某玩家的操作日誌 |

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
| **ldattribute.relic.admin** | **遺物管理** | **OP** |
| **ldattribute.autoattack.admin** | **殺戮管理** | **OP** |
| **ldattribute.anticheat.exempt** | **跳過攻速檢測** | **-** |
| ldattribute.card.decompose | 快捷分解權限 | OP |
| ldattribute.exchange.shop | 商店使用權 | 所有人 |
| soulring.vip | 雙倍掉落 | - |
| soulring.svip | 三倍掉落 | - |
| **ldattribute.autoattack.vip** | **VIP 殺戮參數** | **-** |
| **ldattribute.autoattack.svip** | **SVIP 殺戮參數** | **-** |

---

## 配置文件

所有配置文件位於 `plugins/LD-Attribute/`：

| 文件 / 資料夾 | 說明 |
|---|---|
| config.yml | 主配置（屬性優先級 / 語言 / 數字縮寫） |
| core.yml | 核心模組（存儲方式） |
| value.yml | 自訂義值 |
| scoreboard.yml | 側邊欄 + Tab 頭尾 |
| soulring.yml | 靈魂空間（含過濾白/黑名單） |
| rates.yml | 掉落倍率 |
| item.yml | 卡片定義 |
| cardlevel.yml / star.yml / decompose.yml | 卡片升級 / 升星 / 分解 |
| suit.yml / combo.yml / resonance.yml / bond.yml | 套裝 / 組合 / 共鳴 / 羈絆 |
| collection.yml | 圖鑑 |
| recipe.yml | 合成配方 |
| jewelry.yml | 飾品 |
| talent.yml | 天賦 |
| rune.yml / spells.yml | 符文 / 法術 |
| pet.yml / pet_equipment.yml | 寵物 |
| achievement.yml | 成就 |
| gacha.yml | 抽獎 |
| element.yml / state.yml / tempbuff.yml | 戰鬥 |
| mm.yml | MythicMobs 掉落 |
| ui.yml / stats_gui.yml / page.yml | 介面 |
| command.yml | 指令配置 |
| message/zh_TW.yml | 訊息文字 |
| lang/zh_TW.yml | 屬性別名 |
| 配置/卡片/ | 卡片配置（新版路徑） |
| 配置/魂珠/ | 魂珠類型定義（T1/T2/T3...） |
| 配置/圖鑑/ | 圖鑑配置 + 稱號.yml |
| 配置/遺物/ | 遺物系統（_核心.yml / 套裝.yml / T1遺物/） |
| 配置/掉落/ | 掉落增強（掉落表.yml） |
| 配置/殺戮/ | 殺戮系統（config.yml / 玩家.yml / 反作弊.yml） |
| 配置/兌換商店/ | 商店（每頁一文件） |
| 配置/靈魂空間/分解/ | 靈魂分解規則 |

---

## PlaceholderAPI 變數

### 通用屬性
| 變數 | 說明 |
|---|---|
| %ldattr_<屬性名>% | 玩家屬性值（自動數字縮寫） |
| %ldattr_cards% | 卡片數 |
| %ldattr_points% | 點券 |
| %ldattr_pets% | 寵物數 |
| %ldattr_runes% | 符文數 |
| %ldattr_top_<屬性>_<名次>% | 排行榜玩家名 |

### 圖鑑收藏分 / 稱號
| 變數 | 說明 |
|---|---|
| %ldattr_guide_score% | 玩家收藏分 |
| %ldattr_guide_title% | 當前稱號顯示名 |
| %ldattr_guide_prefix% | 稱號聊天前綴 |

### 魂珠空間
| 變數 | 說明 |
|---|---|
| %ldring_total_<屬性>% | 魂珠屬性總和 |
| %ldring_count_<類型>% | 某類型數量 |
| %ldring_max_<類型>% | 某類型上限 |
| %ldring_slots% | 槽位數 |
| %ldring_pages% | 頁數 |
| %ldring_types% | 類型數 |

### 自訂義值
| 變數 | 說明 |
|---|---|
| %ldvalue_info_<值Id>% | 當前值 |
| %ldvalue_name_<值Id>% | 顯示名 |
| %ldvalue_max_<值Id>% | 最大值 |
| %ldvalue_remain_<值Id>% | 距離最大值還差多少 |
| %ldvalue_has_<值Id>% | 是否有此值 |

### 其他
| 變數 | 說明 |
|---|---|
| %ldsb_...% | 側邊欄 |
| %ldsp_...% | 飾品 |
| %ldtalent_...% | 天賦 |

---

## 存儲方式

修改 `core.yml`：

    storage:
      type: YAML    # YAML / SQLITE / MYSQL

---

## 開發進度

### v1.6.0 新增
- ☑ 掉落增強
- ☑ 遺物系統（動態槽位 / 主副屬性 / 套裝 / 鎖定）
- ☑ 殺戮系統（獨立玩家 / 權限組 / 攻速反作弊）
- ☑ 圖鑑收藏分 + 稱號
- ☑ 統一物品解析器 ItemResolver
- ☑ 中文簡繁轉換 ChineseConverter（1500+ 字對）
- ☑ 變數縮寫 NumberAbbrev
- ☑ 卡片配置熱刷新
- ☑ 靈魂空間批量分解 / 刪除模式
- ☑ 靈魂空間 LORE 白/黑名單
- ☑ 靈魂空間操作日誌（每玩家獨立 + UUID 查詢）
- ☑ 符文鑲嵌成功率 + 失敗處理
- ☑ 魂珠升級成功率 + 失敗處理
- ☑ 魂珠獨立類型配置系統

### 核心 / 既有
- ☑ 核心框架
- ☑ 卡片系統
- ☑ 魂珠空間
- ☑ 飾品背包
- ☑ 天賦加點
- ☑ 怪物圖鑑
- ☑ 自訂義值
- ☑ PAPI 全場景
- ☑ 側邊欄 + Tab 頭尾
- ☑ 靈魂空間
- ☑ 靈魂自動拾取 / 倍率 / 兌換 / 商店
- ☑ 快捷分解
- ☑ SX-Attribute / AttributePlus 對接
- ☑ 符文系統
- ☑ 寵物系統
- ☑ 成就系統
- ☑ 操作日誌

---

## 聯繫

- 作者：LongDrange
- 倉庫：https://github.com/LongDrange/LD-ATTRIBUTE
- 問題回報：https://github.com/LongDrange/LD-ATTRIBUTE/issues
- 最新版本：**v1.6.0**
