const defaultUrl = 'https://script.google.com/macros/s/AKfycbwUjllPl3HqguJUDR58LCalEqF41OarjK0S_3wSjxkp66o3Hr5nEBwmKBsweroFYJVh0g/exec';

module.exports = async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ ok: false, error: 'ใช้คำขอ POST' });
  }

  const { password, action, payload } = req.body || {};
  if (typeof password !== 'string' || !password || typeof action !== 'string') {
    return res.status(400).json({ ok: false, error: 'คำขอไม่ถูกต้อง' });
  }
  const body = JSON.stringify({ type: 'admin', password, action, payload });
  if (body.length > 4096) {
    return res.status(400).json({ ok: false, error: 'คำขอยาวเกินไป' });
  }

  try {
    const response = await fetch(process.env.GOZZA_APPS_SCRIPT_URL || defaultUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
      signal: AbortSignal.timeout(20000)
    });
    if (!response.ok) {
      return res.status(502).json({ ok: false, error: `Apps Script ตอบ HTTP ${response.status}` });
    }
    const result = await response.json();
    if (typeof result?.ok !== 'boolean' || (result.ok && !result.state)) throw Error('Invalid API response');
    return res.status(200).json(result);
  } catch (_) {
    return res.status(502).json({ ok: false, error: 'Apps Script ไม่ส่งผลลัพธ์จาก API' });
  }
};
