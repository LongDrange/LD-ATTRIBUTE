# LD-Attribute 完整文档

> 版本：1.0.0
> 适用：Minecraft 1.12.2 (Paper/Spigot)
> 依赖：Vault / PlaceholderAPI / MythicMobs 4.4.0

---

## 目录
1. 安装
2. 指令大全
3. 配置文件
4. 符文系统
5. 幸运掉落机制
6. PAPI 占位符
7. 数据文件与备份
8. 常见问题

---

## 1. 安装

1. 把 `LD-Attribute-1.0.0.jar` 放入 `plugins/`
2. 启动服务器，插件自动解压默认配置到 `plugins/LD-Attribute/`
3. 编辑配置后执行 `/ldc reload`

**可选依赖**：
- `Vault` - 金币相关功能
- `PlaceholderAPI` - 占位符
- `MythicMobs 4.4.0` - MM 怪掉落

---

## 2. 指令大全

主指令 `/ldc`，别名 `/ldcard` / `/卡牌`。

### 玩家指令

| 指令 | 说明 |
|---|---|
| `/ldc` | 打开卡片背包 |
| `/ldc collection` | 卡片图鉴 |
| `/ldc stats` | 属性面板（分页 + 来源明细） |
| `/ldc merge` | 合成界面 |
| `/ldc top` | 排行榜 |
| `/ldc pet` | 宠物背包 |
| `/ldc pet list` | 宠物列表 |
| `/ldc rune list` | 符文 ID 列表 |
| `/ldc rune collection` | 符文图鉴 |
| `/ldc rune upgrade <ID>` | 符文升级界面（3 换 1） |
| `/ldc rune craft <ID>` | 材料合成符文 |
| `/ldc rune recycle` | 符文回收换点券 |
| `/ldc lang <语言>` | 切换语言 |
| `/ldc mana` | 查看法力 |
| `/ldc help` | 帮助 |

### 管理员指令（`ldattribute.command.admin`）

| 指令 | 说明 |
|---|---|
| `/ldc reload` | 热重载所有配置 |
| `/ldc give <卡ID> [玩家]` | 给卡片 |
| `/ldc remove <卡ID> [玩家]` | 移除卡片 |
| `/ldc givebook <法术ID> [玩家]` | 给法术书 |
| `/ldc star <玩家> <星级>` | 设置卡片星级 |
| `/ldc level <玩家> <等级>` | 设置卡片等级 |
| `/ldc exp <玩家> <经验>` | 设置卡片经验 |
| `/ldc unlock <玩家> <页数>` | 解锁卡片页 |
| `/ldc reset <玩家>` | 重置玩家数据 |
| `/ldc save` | 手动保存 |
| `/ldc backup` | 手动备份 |
| `/ldc debug` | 诊断信息 |
| `/ldc version` | 版本 |

### 符文管理员（op）

| 指令 | 说明 |
|---|---|
| `/ldc rune give <ID> [玩家] [数量]` | 发符文 |
| `/ldc rune socket <孔位> <ID> [槽]` | 直接镶嵌 |
| `/ldc rune clear [槽]` | 清空手持卡符文 |

### 宠物管理员（op）

| 指令 | 说明 |
|---|---|
| `/ldc pet give <ID> [玩家]` | 发宠物蛋 |
| `/ldc pet evolve <ID>` | 进化宠物 |
| `/ldc pet level <玩家> <ID> <值>` | 设置等级 |
| `/ldc pet unlock <玩家> <页数>` | 解锁页 |

---

## 3. 配置文件

目录：`plugins/LD-Attribute/`（改完 `/ldc reload` 生效）

### config.yml
```yaml
AttributePriority:
  攻擊力: 5
  暴擊機率: 8
  # ... 62 个属性

Backup:
  IntervalMinutes: 60    # 每 60 分钟备份
  MaxBackups: 24         # 保留 24 份
```

### cardlevel.yml（卡片升级）
```yaml
Cards:
  孙逊:
    MaxLevel: 10
    MaxStar: 3
    BaseExp: 100
    ExpGrowth: 1.5
    Attributes:
      生命上限:
        Base: 10
        Mode: PERCENT
        Growth: 0.1
        Max: 100
```

### item.yml（卡片定义）
```yaml
孙逊:
  Display: "&e孙逊"
  Material: PAPER
  Type: HERO
  Lore:
  - '&7卡片: &e孙逊'
  - '&c攻击力: +5'
```

### mm.yml（MM 掉落）
```yaml
DropToCards:
  SkeletonKing:
    zhaoyun_t2: 0.01
    "DIAMOND:2:0.5": 1.0

RuneDrops:
  SkeletonKing:
    attack_1: 0.4
  ZOMBIE:
    attack_1: 0.02
```

---

## 4. 符文系统

### 孔位类型
- 攻击孔（ATTACK）
- 防御孔（DEFENSE）
- 法术孔（MAGIC）
- 通用孔（ATTACK/DEFENSE/MAGIC/UTILITY）
- 特殊孔（SPECIAL）

### rune.yml 结构
```yaml
Sockets:
  攻击孔:
    Name: "&c攻击孔"
    AllowedTypes: [ATTACK]
    UnlockCost:
      Points: 5000
      Vault: 10000
      Items: ["DIAMOND:16"]

CardSockets:
  default:
  - "攻击孔"
  - "防御孔"
  - "通用孔"
  孙逊:
  - "攻击孔"
  - "攻击孔"
  - "通用孔"

Runes:
  attack_1:
    Name: "&c攻击符文 I"
    Type: ATTACK
    Material: REDSTONE
    UpgradeTo: "attack_2"
    Recipe:
      Points: 1000
      Items: ["REDSTONE:8", "IRON_INGOT:4"]
    Attributes:
    - "&c攻击力: +10"
    - "&c暴击机率: +2"
```

### 内置 38 符文

| 系列 | ID | 等级 |
|---|---|---|
| 攻击符文 | attack_1 ~ attack_6 | 6 |
| 防御符文 | defense_1 ~ defense_6 | 6 |
| 法术符文 | magic_1 ~ magic_6 | 6 |
| 通用符文 | utility_1 ~ utility_6 | 6 |
| 特殊符文 | special_1 ~ special_6 | 6 |
| 赛季符文 | season_1 ~ season_3 | 3 |
| 活动符文 | event_xmas / event_qixi | 独立 |
| 命运审判 | fate_atk / fate_def / fate_mix | 独立 |

### 玩家操作
```
1. /ldc → 点信息 → 卡片详情
2. 点「✦ 符文孔位」按钮
3. 符文界面：
   - 点【未打孔】→ 消耗材料打孔
   - 点【空】→ 选符文镶嵌
   - 点【已镶嵌】→ 取出
   - Shift+右键【已镶嵌】→ 升级界面
   - 点右下【锁】→ 锁定符文
```

### 回收价目

| 等级 | 点券/个 |
|---|---|
| T1 (_1) | 100 |
| T2 (_2) | 300 |
| T3 (_3) | 800 |
| T4 (_4) | 2000 |
| T5 (_5) | 5000 |
| T6 (_6) | 12000 |
| 赛季 I/II/III | 8k / 20k / 50k |
| 活动 | 5k |
| 命运 | 80k |

### 自定义符文
1. une.yml → `Runes` 加新符文
2. une.yml → `Sockets` 加孔位类型
3. une.yml → `CardSockets` 给卡片配孔位
4. `/ldc reload`

---

## 5. 幸运掉落机制

**基础幸运 = 100%**（每位玩家无需装备）

| 幸运 | 掉落次数 |
|---|---|
| 100% | 1 次 |
| 105% | 1 次 + 5% 概率额外 1 次 |
| 150% | 1 次 + 50% 概率额外 1 次 |
| 200% | 2 次 |
| 300% | 3 次 |
| 350% | 3 次 + 50% 概率额外 1 次 |

**每次独立掷骰**。

**生效范围**：仅 MM 掉落（`DropToCards` + `RuneDrops`）。

---

## 6. PAPI 占位符

需要 `PlaceholderAPI` 插件。

| 占位符 | 说明 |
|---|---|
| `%ldattr_攻击力%` | 玩家当前攻击力 |
| `%ldattr_幸运%` | 当前幸运（默认 100） |
| `%ldattr_top_攻击力_1%` | 攻击力排行第 1 名玩家名 |
| `%ldattr_top_攻击力_1_value%` | 第 1 名的攻击力值 |
| `%ldattr_list%` | 所有属性名 |
| `%ldattr_mana%` | 当前法力 |
| `%ldattr_mana_max%` | 法力上限 |
| `%ldattr_mana_percent%` | 法力百分比 |
| `%ldattr_mana_bar%` | 法力条 |

属性名可用别名（取决于 `lang/*.yml`）。

---

## 7. 数据文件与备份

### 数据位置
```
plugins/LD-Attribute/
  data/
    players.yml
    pets.yml
    rune-collection.yml
    mana.yml
  points.dat
  backups/
    20260924_053000/
      *.yml
      data/
```

### 自动备份
```yaml
Backup:
  IntervalMinutes: 60
  MaxBackups: 24
```

### 手动备份
```
/ldc backup
```

### 恢复
1. 停服务器
2. 覆盖 `backups/时间戳/` 到 `plugins/LD-Attribute/`
3. 启动

---

## 8. 常见问题

**Q: 改配置没生效？** A: `/ldc reload`。改 Java 代码需重编+重启。

**Q: 符文属性不生效？** A: 检查卡片是否镶嵌；`/ldc stats` 看值；属性名大小写/别名匹配。

**Q: MM 怪不掉落？** A: 检查 `mm.yml` 里 mobId 大小写；`/ldc debug` 看 MM 挂钩。

**Q: 备份文件太大？** A: 减小 `MaxBackups` 或增大 `IntervalMinutes`。

**Q: 属性面板慢？** A: Lore 缓存机制已优化，`/ldc reload` 可清缓存。

**Q: 怎么加新属性？** A: 改 Java 代码 + config.yml + lang.yml，需编译。

---

## 附：62 属性列表

**基础**：攻击力 / 破甲 / 暴击机率 / 暴击伤害 / 吸血 / 点燃 / 闪电 / 中毒 / 凋零 / 失明 / 缓速 / 真实伤害 / 命中率 / 闪避 / 格挡 / 防御力 / 韧性 / 反射 / 经验加成 / 事件讯息 / 生命上限 / 移动速度 / 速度百分比

**扩展**：PVP攻击力 / PVE攻击力 / 远程伤害 / 攻击速度 / 暴击抵抗 / 燃烧几率 / 燃烧伤害 / PVP防御 / PVE防御 / 生命恢复 / 吸血抵抗 / 击退抗性 / 远程护甲 / 法力上限 / 法力恢复 / 技能极速 / 幸运 / 箭矢速度 / 箭矢精准 / 箭矢穿透数 / 盾牌减伤 / 法术伤害 / 法术防御 / 法术暴击几率 / 法术暴击倍率 / 法术穿透 / 法术吸血

**符文扩展**：暴伤抵抗 / 伤害加成 / 生命加成 / 防御加成 / 吸血几率 / 吸血倍率 / 附加伤害 / 反伤倍率 / 反伤抵抗

---

> 文档更新：2026-09-24
