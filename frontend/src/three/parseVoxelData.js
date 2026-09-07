export function parseVoxelData(raw) {
  if (!Array.isArray(raw) || raw.length === 0) {
    return { blocks: [], count: 0, invalid: 0 }
  }

  const blocks = []
  let invalid = 0

  for (const item of raw) {
    if (!Array.isArray(item) || item.length !== 4) {
      invalid += 1
      continue
    }

    const [id, x, y, z] = item

    if (typeof id !== 'string' || id.length === 0) {
      invalid += 1
      continue
    }

    if (!Number.isFinite(x) || !Number.isFinite(y) || !Number.isFinite(z)) {
      invalid += 1
      continue
    }

    blocks.push({ id, x, y, z })
  }

  return { blocks, count: blocks.length, invalid }
}