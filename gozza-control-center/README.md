# Gozza Control Center

Google Sheets เก็บรหัสแอดมิน รายการอุปกรณ์ และสถานะอนุญาต Vercel เสิร์ฟหน้า Control Center และส่งคำขอแอดมินจาก `/api/admin` ไปยัง Apps Script ส่วน APK เรียก Apps Script โดยตรง ทั้งสองทางใช้ JSON

1. ในโปรเจกต์ Apps Script ของ Gozza ให้วาง `Code.gs` และ `appsscript.json` จากโฟลเดอร์ `apps-script` แล้วลบไฟล์ `Index.html` ที่เคยอัปโหลดไว้ใน Apps Script
2. รัน `setupDatabase` หนึ่งครั้งจาก editor และกดอนุญาต ระบบใช้ `SPREADSHEET_ID` เดิมถ้ามี โดยเพิ่มคอลัมน์ท้ายชีต Devices และเก็บข้อมูลเดิม หากไม่มีจะสร้างชีตใหม่ เปิด URL ของชีตจาก Execution log
3. ดูหรือเปลี่ยนรหัสแอดมินที่ `Settings` แถว `admin_password` ใช้ชีตที่แชร์เฉพาะผู้ดูแล
4. Deploy > Manage deployments > Edit > New version โดยใช้ deployment เดิม ตั้ง Execute as Me และ Who has access Anyone เพื่อให้ APK ลงทะเบียนได้โดยไม่ต้องลงชื่อเข้าใช้ Google
5. เปิด URL `/exec` แล้วตรวจว่าตอบ JSON ที่มี `apiVersion: 2`
6. ใน Vercel เลือก repo เดิม ตั้ง Root Directory เป็น `gozza-control-center/frontend` และ Framework Preset เป็น Other ใช้ค่าเริ่มต้นสำหรับ Build Command และ Output Directory เพื่อเสิร์ฟ `index.html` และฟังก์ชัน `api/admin.js`
7. เปิดเว็บ Vercel แล้วเข้าสู่ระบบด้วยรหัสในชีต หากเปลี่ยน URL `/exec` ให้ตั้ง `GOZZA_APPS_SCRIPT_URL` ใน Vercel และแก้ `TesterAccess.kt` ก่อน build APK

APK รุ่น 0.1.51 ส่งรหัส SHA-256 เดิม รุ่นเครื่อง และชื่ออุปกรณ์เมื่อเริ่มแอปและประมาณทุก 2 นาที รหัสใหม่สร้างหนึ่งแถวที่ `enabled=true` ส่วนรหัสเดิมอัปเดตเฉพาะข้อมูลเครื่องและเวลาเชื่อมต่อ การระงับรายเครื่องและสวิตช์รวมมีผลเมื่อแอปตรวจรอบถัดไป ตัวเลขเชื่อมต่อล่าสุดนับเครื่องที่ตรวจสิทธิ์ใน 5 นาทีที่ผ่านมา

เปิดใช้ backend และทดสอบก่อนแจก APK รุ่นใหม่ เครื่องที่ใช้ APK 0.1.50 หรือต่ำกว่ายังอ่าน `control/access.json` จาก GitHub และยังไม่ปรากฏในชีตอัตโนมัติ ค่าที่มีอยู่ในชีตเดิมจะถูกเก็บไว้ การระงับรายเครื่องควรใช้ปุ่มระงับ เพราะลบแถวด้วยมือจะทำให้เครื่องนั้นลงทะเบียนใหม่ได้
