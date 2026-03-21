function parseChunk(chunk) {
  const trimmed = String(chunk || '').trim();
  if (!trimmed) return null;
  const colonIndex = trimmed.indexOf(':');
  if (colonIndex > 0) {
    const key = trimmed.slice(0, colonIndex).trim().toUpperCase();
    const text = trimmed.slice(colonIndex + 1).trim();
    return { key, text, raw: `${key}. ${text}` };
  }
  const dotIndex = trimmed.indexOf('.');
  if (dotIndex > 0) {
    const key = trimmed.slice(0, dotIndex).trim().toUpperCase();
    const text = trimmed.slice(dotIndex + 1).trim();
    return { key, text, raw: `${key}. ${text}` };
  }
  return { key: trimmed, text: trimmed, raw: trimmed };
}

export function parseLabeledOptions(raw) {
  const source = String(raw || '').trim();
  if (!source) return [];

  const matches = [...source.matchAll(/([A-Za-z])\s*[.:：]\s*/g)];
  if (matches.length > 0 && matches[0].index === 0) {
    const list = matches.map((match, index) => {
      const key = String(match[1] || '').toUpperCase();
      const start = (match.index || 0) + match[0].length;
      const end = index < matches.length - 1 ? (matches[index + 1].index || source.length) : source.length;
      const text = source.slice(start, end).replace(/^[,\s]+|[,\s]+$/g, '').trim();
      return text ? { key, text, raw: `${key}. ${text}` } : null;
    }).filter(Boolean);
    if (list.length > 0) return list;
  }

  return source
    .split(',')
    .map(chunk => parseChunk(chunk))
    .filter(Boolean);
}

export function parseOptionDisplayItems(raw) {
  return parseLabeledOptions(raw).map(item => item.raw);
}
