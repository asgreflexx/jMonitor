import { useMemo, useState } from 'react'
import type { FlameNode } from '../api/client'

const ROW_HEIGHT = 20
const MIN_FRACTION = 0.002 // hide slivers narrower than 0.2%

interface Box {
  node: FlameNode
  depth: number
  x: number // fraction [0,1]
  width: number // fraction
  path: string
}

/** Flattens the tree into positioned boxes relative to `total` samples. */
function layout(root: FlameNode, total: number): { boxes: Box[]; maxDepth: number } {
  const boxes: Box[] = []
  let maxDepth = 0
  const walk = (node: FlameNode, depth: number, x: number, path: string) => {
    const width = node.value / total
    if (width < MIN_FRACTION) return
    boxes.push({ node, depth, x, width, path })
    maxDepth = Math.max(maxDepth, depth)
    let childX = x
    for (let i = 0; i < node.children.length; i++) {
      walk(node.children[i], depth + 1, childX, `${path}/${i}`)
      childX += node.children[i].value / total
    }
  }
  walk(root, 0, 0, '0')
  return { boxes, maxDepth }
}

/** Deterministic warm color from a frame name. */
function colorFor(name: string): string {
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) & 0xffff
  const hue = 20 + (h % 40) // 20–60: reds→oranges→yellows
  const sat = 55 + (h % 25)
  return `hsl(${hue}, ${sat}%, 55%)`
}

/**
 * Dependency-free flame graph with click-to-zoom (Phase 5). The parent passes a
 * `key` per recording so focus state resets naturally on a new recording.
 */
export function FlameGraph({ root }: { root: FlameNode }) {
  const [focus, setFocus] = useState<FlameNode>(root)

  const total = focus.value || 1
  const { boxes, maxDepth } = useMemo(() => layout(focus, total), [focus, total])
  const height = (maxDepth + 1) * ROW_HEIGHT

  if (root.value === 0) {
    return <div className="detail__status">No CPU samples in this recording.</div>
  }

  return (
    <>
      <div className="toolbar">
        <button
          type="button"
          className="btn btn--small"
          disabled={focus === root}
          onClick={() => setFocus(root)}
        >
          Reset zoom
        </button>
        <span className="toolbar__info">
          {root.value.toLocaleString()} samples · click a frame to zoom
        </span>
      </div>
      <div className="flame" style={{ height }}>
        {boxes.map((b) => {
          const pct = ((b.node.value / root.value) * 100).toFixed(1)
          return (
            <div
              key={b.path}
              className="flame__frame"
              style={{
                left: `${b.x * 100}%`,
                width: `${b.width * 100}%`,
                top: b.depth * ROW_HEIGHT,
                height: ROW_HEIGHT - 1,
                background: colorFor(b.node.name),
              }}
              title={`${b.node.name} — ${b.node.value.toLocaleString()} samples (${pct}%)`}
              onClick={() => setFocus(b.node)}
            >
              <span className="flame__label">{b.node.name}</span>
            </div>
          )
        })}
      </div>
    </>
  )
}
