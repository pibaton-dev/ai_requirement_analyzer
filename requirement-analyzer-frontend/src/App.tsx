import { useEffect, useState } from 'react'
import { api } from './lib/api'

type Health = {
  status: string
  service: string
}

export default function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    api.get<Health>('/health')
      .then(({ data }) => setHealth(data))
      .catch(() => setFailed(true))
  }, [])

  return (
    <main className="workspace">
      <header className="topbar">
        <a className="wordmark" href="/" aria-label="AI Requirement Analyzer home">
          <span className="wordmark-mark" aria-hidden="true">RA</span>
          <span>Requirement Analyzer</span>
        </a>
        <span className="environment-label">LOCAL ENVIRONMENT</span>
      </header>

      <section className="intro" aria-labelledby="page-title">
        <p className="eyebrow">PROJECT WORKSPACE</p>
        <h1 id="page-title">Turn project intent into clear requirements.</h1>
        <p className="intro-copy">The analysis workspace is being assembled. Generated requirements will remain drafts until a person reviews them.</p>
      </section>

      <section className="service-panel" aria-labelledby="service-title">
        <div>
          <p className="eyebrow">MILESTONE 1</p>
          <h2 id="service-title">Platform services</h2>
        </div>
        <div className="service-row">
          <span>Spring API</span>
          <span className={`service-status ${health ? 'is-up' : failed ? 'is-down' : ''}`} role="status">
            <span className="status-dot" />
            {health ? 'Online' : failed ? 'Unavailable' : 'Checking'}
          </span>
        </div>
      </section>
    </main>
  )
}