// Left navigation rail.
//
// Phase 0: static shell only. From Phase 1 the process list (discovered via the
// Attach API) is rendered here and selecting an entry drives the main content.

export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__logo">jM</span>
        <span className="sidebar__title">jMonitor</span>
      </div>

      <nav className="sidebar__nav">
        <div className="sidebar__section">Processes</div>
        <p className="sidebar__empty">
          Process discovery arrives in Phase&nbsp;1.
        </p>
      </nav>

      <div className="sidebar__footer">v0.1.0 · scaffold</div>
    </aside>
  )
}
