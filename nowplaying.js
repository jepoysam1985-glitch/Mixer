// Vercel serverless function: /api/nowplaying
// POST (mula sa JEIANNE FM Mixer, may NP_KEY) -> sine-save ang title/artist/cover
// GET  (mula sa website)                       -> ibinabalik ang kasalukuyang tugtog
const URL_ = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
const TOKEN = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
const KEY = 'jeiannefm:nowplaying';

async function redis(cmd) {
  const r = await fetch(URL_, {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + TOKEN, 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd),
  });
  if (!r.ok) throw new Error('redis ' + r.status);
  return (await r.json()).result;
}

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Cache-Control', req.method === 'GET' ? 'public, s-maxage=8, stale-while-revalidate=15' : 'no-store');
  if (req.method === 'OPTIONS') return res.status(204).end();
  if (!URL_ || !TOKEN) return res.status(500).json({ error: 'Database not connected' });

  try {
    if (req.method === 'POST') {
      let b = req.body;
      if (typeof b === 'string') { try { b = JSON.parse(b); } catch { b = {}; } }
      b = b || {};
      if (!process.env.NP_KEY || b.key !== process.env.NP_KEY) return res.status(401).json({ error: 'Bad key' });
      const data = b.on === false
        ? { on: false, ts: Date.now() }
        : {
            on: true,
            title: String(b.title || '').slice(0, 200),
            artist: String(b.artist || '').slice(0, 200),
            label: String(b.label || '').slice(0, 300),
            cover: typeof b.cover === 'string' && b.cover.startsWith('data:image/') && b.cover.length < 600000 ? b.cover : '',
            ts: Date.now(),
          };
      await redis(['SET', KEY, JSON.stringify(data), 'EX', 21600]);
      return res.status(200).json({ ok: true });
    }
    if (req.method === 'GET') {
      const v = await redis(['GET', KEY]);
      return res.status(200).json(v ? JSON.parse(v) : { on: false });
    }
    return res.status(405).json({ error: 'Method not allowed' });
  } catch (e) {
    return res.status(500).json({ error: 'Server error' });
  }
};
