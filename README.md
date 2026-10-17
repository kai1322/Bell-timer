# Bell Timer

แอป Android แจ้งเตือนซ้ำด้วยเสียงระฆัง "ปิ๊ง ปิ๊ง"  
**ไม่มีโฆษณา** ใช้ `AlarmManager` exact alarm

## คุณสมบัติ

- ตั้งช่วงเวลา 1 นาที – 4 ชั่วโมง
- เตือนซ้ำไม่หยุดจนกว่าจะกดหยุดเอง
- เสียง "ปิ๊ง" สร้างในแอปเอง (sine wave) ปรับจำนวนครั้ง 1–3 และความดังได้
- นับเวลาจากตอนกดเริ่ม (ไม่สะสม error)
- แถบแจ้งเตือนเงียบ แสดงเวลาครั้งถัดไป + ปุ่มหยุด
- ตั้งต่ออัตโนมัติหลังรีสตาร์ท
- ปุ่มยกเว้นจำกัดแบตเตอรี่ + เปิดหน้าเริ่มอัตโนมัติสำหรับ Xiaomi/Redmi (MIUI)

## วิธี Build APK ด้วย GitHub Actions

1. สร้าง repo ใหม่บน GitHub ชื่อ `Bell-timer` (หรืออะไรก็ได้)
2. อัปโหลดไฟล์ทั้งหมดในโฟลเดอร์นี้ขึ้น GitHub (git push)
3. ไปที่แท็บ **Actions** → เลือก workflow **Build APK** → **Run workflow**
4. รอจนเสร็จ แล้วดาวน์โหลด artifact ชื่อ `Bell-timer-debug` จะได้ไฟล์ `app-debug.apk`
5. ติดตั้งบนมือถือ (อนุญาตติดตั้งจากแหล่งที่ไม่รู้จัก)

หรือใช้คำสั่ง:

```bash
git init
git add .
git commit -m "Initial Bell-timer"
git branch -M main
git remote add origin https://github.com/<your-username>/Bell-timer.git
git push -u origin main
```

จากนั้นไปรัน Actions ตามด้านบน

## การใช้งานครั้งแรก (สำคัญสำหรับ Xiaomi/Redmi)

1. ติดตั้ง APK แล้วเปิดแอป
2. อนุญาต **การแจ้งเตือน**
3. กดปุ่ม **ยกเว้นจำกัดแบตเตอรี่** → เลือก "ไม่จำกัด" / "No restrictions"
4. กดปุ่ม **เปิดหน้าเริ่มอัตโนมัติ (MIUI)** → หาแอป Bell Timer แล้วเปิดสวิตช์
5. (แนะนำ) ล็อกแอปใน Recent apps ด้วย
6. ตั้งช่วงเวลา แล้วกด **เริ่ม**

## โครงสร้างหลัก

- `AlarmScheduler` – ใช้ `setExactAndAllowWhileIdle`
- `AlarmReceiver` – เล่นเสียง + คำนวณรอบถัดไปจาก `startTime + n * interval`
- `BootReceiver` – ตั้งต่อหลังรีบูต
- `TimerService` – Foreground service เบา ๆ เพื่อความเสถียรบน OEM เข้มงวด
- `SoundPlayer` – สร้างเสียงปิ๊งด้วย AudioTrack sine wave

## License

MIT – ใช้งานได้อย่างอิสระ
