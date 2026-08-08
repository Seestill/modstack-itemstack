# ModStack ItemStack (Fabric 1.20.1)

มอด Fabric สำหรับ Minecraft **1.20.1** ที่รวมระบบ:

1. **Mob Stacking** — มอบชนิดเดียวกันที่อยู่ใกล้กันจะรวมเป็น "กอง" เดียว แสดงชื่อ `ZombieName x5` แทนการมีมอบวิ่งกระจายเป็นสิบๆ ตัว (ลด lag)
2. **Item Stacking** — ปรับ max stack size ของไอเทมได้ตามใจ รวมถึงไอเทมที่ปกติ stack ไม่ได้ เช่น ดาบ, ธนู, shield
3. **Breeding Mechanics** — เมื่อสัตว์สองตัวผสมพันธุ์กันและเป็นมอบที่ stack อยู่ จะ**เพิ่มจำนวนในกอง**แทนที่จะ spawn ลูกสัตว์ตัวใหม่วิ่งไปมา (กันฟาร์มผสมพันธุ์ทำ lag)

## โครงสร้างโปรเจกต์

```
modstack/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE
└── src/main/
    ├── java/com/modstack/
    │   ├── ModStackMod.java          # entrypoint หลัก (server+common)
    │   ├── ModStackClient.java       # entrypoint client
    │   ├── config/ModStackConfig.java# ค่าคอนฟิกทั้งหมด แก้ตรงนี้ที่เดียว
    │   ├── entity/StackAccess.java   # interface เชื่อม mixin ต่างๆ
    │   └── mixin/
    │       ├── ItemAccessorMixin.java     # เปิดให้แก้ max stack ของ Item
    │       ├── MobEntityStackMixin.java   # ระบบ stack มอบ + ตายทีละตัว
    │       └── AnimalBreedMixin.java      # ผสมพันธุ์ -> เพิ่มจำนวนในกอง
    └── resources/
        ├── fabric.mod.json
        └── modstack.mixins.json
```

## วิธี build

ต้องมี JDK 17+ และเน็ตสำหรับดาวน์โหลด dependency (Gradle จะโหลด Minecraft, Yarn mappings, Fabric API เอง):

```bash
cd modstack
./gradlew build
```

ได้ไฟล์ jar ที่ `build/libs/modstack-itemstack-1.0.0.jar` — เอาไปวางใน `mods/` ของ Fabric Loader 1.20.1 (ต้องลง [Fabric API](https://modrinth.com/mod/fabric-api) ด้วย)

ถ้ายังไม่มี `gradlew` (gradle wrapper) ในเครื่อง ให้รัน:
```bash
gradle wrapper --gradle-version 8.8
```
ก่อน แล้วค่อย `./gradlew build`

## จุดที่ปรับแต่งได้ (`ModStackConfig.java`)

| ตัวแปร | ความหมาย |
|---|---|
| `MAX_MOB_STACK` | จำนวนมอบสูงสุดต่อกอง (default 64) |
| `MERGE_RADIUS` | รัศมี (บล็อก) ที่มอบจะรวมกัน |
| `MERGE_INTERVAL_TICKS` | ความถี่ในการสแกนรวมกอง |
| `ITEM_STACK_OVERRIDES` | Map ของ item id -> max stack size ใหม่ |
| `BREEDING_ADDS_TO_STACK` | true = ผสมพันธุ์แล้วเพิ่มจำนวนในกองแทนการ spawn ลูก |
| `BONUS_OFFSPRING_CHANCE` | โอกาสได้โบนัส +2 แทน +1 ตอนผสมพันธุ์ |

## หลักการทำงาน (technical)

- **Mob stacking** ใช้ Mixin เข้า `MobEntity`: เก็บ `modstack_count` เป็น field ธรรมดา + เขียน/อ่านลง NBT (`ModStackCount`) เพื่อให้รอด save/load, ทุกๆ `MERGE_INTERVAL_TICKS` จะสแกนหา entity ชนิดเดียวกันในรัศมีแล้ว merge เข้าเป็นกองเดียว, ตอนโดน damage ที่จะถึงตาย จะ intercept ไว้ที่ `damage()` — ลด count ทีละ 1, รัน loot table ของตัวที่ "หลุด" จากกองจริง (ผ่าน `dropLoot` invoker), แล้วฮีลตัวที่เหลือเต็มแทนที่จะให้ทั้งกองตายพร้อมกัน
- **Item stacking** ใช้ Accessor Mixin เปิดให้แก้ private field `maxCount` ของ `Item` ได้ตรงๆ ตอน mod init เลย ไม่ต้อง re-register item ใหม่
- **Breeding** ใช้ Mixin แทรกที่ `AnimalEntity#breed()` ก่อน vanilla logic — ถ้าทั้งคู่เป็นชนิดเดียวกันและใช้ระบบ stack อยู่ จะยกเลิก (`cancel`) การ spawn ลูกของ vanilla แล้วเพิ่มจำนวนในกองของตัวแม่/พ่อแทน พร้อม reset breeding cooldown ให้เหมือนเดิม

## ข้อจำกัดที่รู้อยู่แล้ว (ปรับต่อได้)

- Mixin เข้า `MobEntity` ครอบคลุมมอบส่วนใหญ่ (zombie, skeleton, cow, pig, ...) แต่ไม่รวม `PlayerEntity` (ไม่ควร stack อยู่แล้ว) และไม่รวม boss เช่น Ender Dragon/Wither ซึ่งดีอยู่แล้วที่ไม่ควร stack
- ระบบ loot ตอน "pop" ทีละตัวใช้ loot table จริงของมอบ แต่ยังไม่ได้คำนวณ looting enchant bonus แยกจาก vanilla — ต่อยอดได้โดยส่ง `DamageSource`/looting level เข้า `dropLoot` ให้ครบ
- ยังไม่มี GUI/command สำหรับดู-ปรับ config ระหว่างเล่น (ตอนนี้ config เป็นค่าคงที่ในโค้ด) — ถ้าต้องการ config ผ่านไฟล์ JSON ที่แก้ได้โดยไม่ต้อง recompile บอกได้ จะเพิ่ม Cloth Config หรือ custom JSON loader ให้
