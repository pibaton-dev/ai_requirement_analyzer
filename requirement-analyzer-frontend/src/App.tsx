import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router'
import { api } from './lib/api'

type Health = {
  status?: string
  service?: string
}

type User = {
  id: string
  email: string
  displayName: string
  status: string
}

type AuthState = {
  token: string
  expiresAt: string
  user: User
}

type Project = {
  id: string
  name: string
  description?: string | null
  domain?: string | null
  ownerId: string
  status: string
  createdAt: string
  updatedAt: string
}

type AnalysisJob = {
  id: string
  projectId: string
  ownerId: string
  text: string
  documentIds: string[]
  status: 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'
  correlationId?: string | null
  summary?: string | null
  errorMessage?: string | null
  createdAt: string
  updatedAt: string
  startedAt?: string | null
  completedAt?: string | null
}

type ProjectDocument = {
  id: string
  projectId: string
  fileName: string
  contentType: string
  fileSize: number
  status: string
  createdAt: string
}

type ReviewVersion = {
  id: string
  projectId: string
  analysisJobId: string
  versionNumber: number
  title: string
  summary?: string | null
  status: 'DRAFT' | 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}

type Requirement = {
  id: string
  requirementId: string
  title: string
  description?: string | null
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'MANUAL'
  sourceType: 'AI_GENERATED' | 'MANUAL' | 'USER_EDITED'
  createdAt: string
  updatedAt: string
}

type Question = {
  id: string
  questionId: string
  question: string
  relatedRequirementId?: string | null
  reason?: string | null
  impact?: string | null
  status: 'OPEN' | 'ANSWERED'
  answer?: string | null
  createdAt: string
  updatedAt: string
}

type AuditEvent = {
  id: string
  action: string
  targetType?: string | null
  targetKey?: string | null
  previousStatus?: string | null
  newStatus?: string | null
  details?: string | null
  createdAt: string
}

type ReviewSummary = {
  version: ReviewVersion
  requirements: Requirement[]
  questions: Question[]
  auditEvents: AuditEvent[]
}

const STORAGE_KEY = 'requirement-analyzer-auth'

const authHeader = (token: string) => ({ Authorization: `Bearer ${token}` })

const getStoredAuth = (): AuthState | null => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthState
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

const persistAuth = (state: AuthState | null) => {
  if (!state) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }

  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

const toItems = <T,>(payload: T[] | { items?: T[]; content?: T[]; data?: T[] } | undefined): T[] => {
  if (Array.isArray(payload)) return payload
  if (!payload) return []
  const maybe = payload as { items?: T[]; content?: T[]; data?: T[] }
  return maybe.items ?? maybe.content ?? maybe.data ?? []
}

export default function App() {
  return (
    <BrowserRouter>
      <AppShell />
    </BrowserRouter>
  )
}

function AppShell() {
  const [auth, setAuth] = useState<AuthState | null>(() => getStoredAuth())
  const [health, setHealth] = useState<Health | null>(null)

  useEffect(() => {
    api.get<Health>('/health')
      .then(({ data }) => setHealth(data))
      .catch(() => setHealth(null))
  }, [])

  useEffect(() => {
    persistAuth(auth)
  }, [auth])

  if (!auth) {
    return <LandingPage health={health} onAuthenticated={setAuth} />
  }

  return <DashboardRouter auth={auth} onSignOut={() => setAuth(null)} />
}

function DashboardRouter({ auth, onSignOut }: { auth: AuthState; onSignOut: () => void }) {
  return (
    <Routes>
      <Route path="/" element={<DashboardPage auth={auth} onSignOut={onSignOut} />} />
      <Route path="/projects/:projectId" element={<ProjectDetailPage auth={auth} onSignOut={onSignOut} />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function LandingPage({ health, onAuthenticated }: { health: Health | null; onAuthenticated: (state: AuthState) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const endpoint = mode === 'login' ? '/auth/login' : '/auth/register'
      const payload = mode === 'login'
        ? { email, password }
        : { email, password, displayName }

      const { data } = await api.post(endpoint, payload)
      onAuthenticated({
        token: data.accessToken,
        expiresAt: data.expiresAt,
        user: data.user,
      })
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to complete the request.'
      setError(message || 'Unable to complete the request.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="workspace shell">
      <header className="topbar">
        <div className="wordmark" aria-label="AI Requirement Analyzer home">
          <span className="wordmark-mark" aria-hidden="true">RA</span>
          <span>Requirement Analyzer</span>
        </div>
        <span className="environment-label">LOCAL ENVIRONMENT</span>
      </header>

      <section className="hero-grid">
        <div className="hero-copy">
          <p className="eyebrow">PROJECT WORKSPACE</p>
          <h1>Requirement Analyzer</h1>
          <p className="intro-copy">
            Turn project intent into structured requirements, review drafts, and keep every decision traceable.
          </p>

          <div className="status-card" aria-live="polite">
            <span className="status-label">Spring API</span>
            <span className={`service-status ${health ? 'is-up' : 'is-down'}`} role="status">
              <span className="status-dot" />
              {health ? 'Online' : 'Unavailable'}
            </span>
          </div>
        </div>

        <div className="panel auth-panel">
          <div className="segmented" aria-label="Authentication mode selector">
            <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
              Sign in
            </button>
            <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
              Register
            </button>
          </div>

          <form onSubmit={handleSubmit} className="stacked-form">
            {mode === 'register' && (
              <label>
                <span>Display name</span>
                <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Ada Review" />
              </label>
            )}

            <label>
              <span>Email</span>
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="team@example.com" required />
            </label>

            <label>
              <span>Password</span>
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="••••••••" required />
            </label>

            {error && <p className="form-error">{error}</p>}
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? 'Working...' : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>
        </div>
      </section>
    </main>
  )
}

function DashboardPage({ auth, onSignOut }: { auth: AuthState; onSignOut: () => void }) {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [domain, setDomain] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const loadProjects = async () => {
    const { data } = await api.get('/projects', { headers: authHeader(auth.token) })
    setProjects(toItems<Project>(data))
  }

  useEffect(() => {
    loadProjects().catch(() => setError('Unable to load your projects.'))
  }, [auth.token])

  const submitProject = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSaving(true)

    try {
      const { data } = await api.post('/projects', { name, description, domain }, { headers: authHeader(auth.token) })
      setName('')
      setDescription('')
      setDomain('')
      navigate(`/projects/${data.id}`)
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to create the project.'
      setError(message || 'Unable to create the project.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="workspace shell">
      <header className="topbar app-topbar">
        <div className="wordmark" aria-label="Requirement Analyzer home">
          <span className="wordmark-mark" aria-hidden="true">RA</span>
          <span>Requirement Analyzer</span>
        </div>
        <div className="top-actions">
          <span className="user-chip">{auth.user.displayName}</span>
          <button type="button" className="ghost-button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <section className="layout-two-column">
        <div className="panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">DASHBOARD</p>
              <h2>Projects</h2>
            </div>
          </div>

          <div className="project-list">
            {projects.length === 0 ? (
              <p className="muted">No projects yet. Create your first one to begin analysis.</p>
            ) : (
              projects.map((project) => (
                <Link key={project.id} to={`/projects/${project.id}`} className="project-card">
                  <div>
                    <strong>{project.name}</strong>
                    <p>{project.description || 'No description yet.'}</p>
                  </div>
                  <span className="pill">{project.status}</span>
                </Link>
              ))
            )}
          </div>
        </div>

        <aside className="panel">
          <p className="eyebrow">NEW PROJECT</p>
          <h3>Create a workspace</h3>
          <form onSubmit={submitProject} className="stacked-form compact">
            <label>
              <span>Project name</span>
              <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Attendance platform" required />
            </label>
            <label>
              <span>Description</span>
              <textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Outline the problem and project context" rows={4} />
            </label>
            <label>
              <span>Domain</span>
              <input value={domain} onChange={(event) => setDomain(event.target.value)} placeholder="Education" />
            </label>
            {error && <p className="form-error">{error}</p>}
            <button type="submit" className="primary-button" disabled={saving}>
              {saving ? 'Creating...' : 'Create project'}
            </button>
          </form>
        </aside>
      </section>
    </main>
  )
}

function ProjectDetailPage({ auth, onSignOut }: { auth: AuthState; onSignOut: () => void }) {
  const { projectId } = useParams()
  const [project, setProject] = useState<Project | null>(null)
  const [isLoadingProject, setIsLoadingProject] = useState(true)
  const [documents, setDocuments] = useState<ProjectDocument[]>([])
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<string[]>([])
  const [fileInputKey, setFileInputKey] = useState(0)
  const [jobs, setJobs] = useState<AnalysisJob[]>([])
  const [selectedJob, setSelectedJob] = useState<AnalysisJob | null>(null)
  const [text, setText] = useState('')
  const [version, setVersion] = useState<ReviewVersion | null>(null)
  const [review, setReview] = useState<ReviewSummary | null>(null)
  const [requirementForm, setRequirementForm] = useState({ requirementId: 'REQ-001', title: '', description: '', priority: 'HIGH', status: 'PENDING_REVIEW' })
  const [questionForm, setQuestionForm] = useState({ questionId: 'Q-001', question: '', relatedRequirementId: '', reason: '', impact: '' })
  const [answerText, setAnswerText] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [exportingFormat, setExportingFormat] = useState<'json' | 'pdf' | 'xlsx' | null>(null)

  const loadProject = useMemo(() => async () => {
    if (!projectId) return

    setError('')
    const projectResponse = await api.get<Project>(`/projects/${projectId}`, { headers: authHeader(auth.token) })
    setProject(projectResponse.data)

    const [jobsResponse, documentsResponse] = await Promise.all([
      api.get(`/projects/${projectId}/analyses`, { headers: authHeader(auth.token) }),
      api.get(`/projects/${projectId}/documents`, { headers: authHeader(auth.token) }),
    ])
    const nextJobs = toItems<AnalysisJob>(jobsResponse.data)
    setDocuments(toItems<ProjectDocument>(documentsResponse.data))
    setJobs(nextJobs)
    if (nextJobs.length > 0) {
      setSelectedJob((current) => current ?? nextJobs[nextJobs.length - 1])
    }
  }, [auth.token, projectId])

  useEffect(() => {
    setIsLoadingProject(true)
    setProject(null)
    setDocuments([])
    setSelectedDocumentIds([])
    setJobs([])
    setSelectedJob(null)
    setVersion(null)
    setReview(null)
  }, [projectId])

  const uploadDocument = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!projectId) return

    const fileInput = event.currentTarget.elements.namedItem('document') as HTMLInputElement | null
    const file = fileInput?.files?.[0]
    if (!file) {
      setError('Choose a document to upload.')
      return
    }

    setError('')
    setIsUploading(true)
    try {
      const body = new FormData()
      body.append('file', file)
      const { data } = await api.post<ProjectDocument>(`/projects/${projectId}/documents`, body, {
        headers: authHeader(auth.token),
      })
      setDocuments((current) => [data, ...current])
      setFileInputKey((current) => current + 1)
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to upload the document.'
      setError(message || 'Unable to upload the document.')
    } finally {
      setIsUploading(false)
    }
  }

  const deleteDocument = async (documentId: string) => {
    setError('')
    try {
      await api.delete(`/documents/${documentId}`, { headers: authHeader(auth.token) })
      setDocuments((current) => current.filter((document) => document.id !== documentId))
      setSelectedDocumentIds((current) => current.filter((id) => id !== documentId))
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to delete the document.'
      setError(message || 'Unable to delete the document.')
    }
  }

  useEffect(() => {
    loadProject()
      .catch(() => setError('Unable to load project data.'))
      .finally(() => setIsLoadingProject(false))
  }, [loadProject])

  useEffect(() => {
    if (!selectedJob || !projectId || !['QUEUED', 'PROCESSING'].includes(selectedJob.status)) return

    const timeout = window.setTimeout(async () => {
      try {
        const { data } = await api.get<AnalysisJob>(`/analyses/${selectedJob.id}`, { headers: authHeader(auth.token) })
        setSelectedJob(data)
        setJobs((current) => current.map((job) => (job.id === data.id ? data : job)))
      } catch {
        // Ignore transient polling errors and keep the last known status.
      }
    }, 1500)

    return () => window.clearTimeout(timeout)
  }, [auth.token, projectId, selectedJob])

  const refreshReview = async (nextVersionId: string) => {
    const { data } = await api.get<ReviewSummary>(`/projects/${projectId}/analysis-versions/${nextVersionId}/review`, { headers: authHeader(auth.token) })
    setReview(data)
    setVersion(data.version)
  }

  const submitAnalysis = async (event: FormEvent) => {
    event.preventDefault()
    if (!projectId) return
    if (!text.trim() && selectedDocumentIds.length === 0) {
      setError('Enter analysis text or select at least one document.')
      return
    }

    setError('')
    setIsSubmitting(true)

    try {
      const { data } = await api.post(`/projects/${projectId}/analyses`, {
        text,
        documentIds: selectedDocumentIds,
      }, { headers: authHeader(auth.token) })
      setSelectedJob(data)
      setJobs((current) => [data, ...current])
      setText('')
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to submit the analysis.'
      setError(message || 'Unable to submit the analysis.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const createVersion = async () => {
    if (!projectId || !selectedJob) return
    try {
      const { data } = await api.post(`/projects/${projectId}/analyses/${selectedJob.id}/versions`, {}, { headers: authHeader(auth.token) })
      setVersion(data)
      await refreshReview(data.id)
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to create the review version.'
      setError(message || 'Unable to create the review version.')
    }
  }

  const exportVersion = async (format: 'json' | 'pdf' | 'xlsx') => {
    if (!projectId || !version) return

    setExportingFormat(format)
    try {
      const response = await api.get(`/projects/${projectId}/analysis-versions/${version.id}/export/${format}`, {
        headers: authHeader(auth.token),
        responseType: 'blob',
      })

      const contentTypes = {
        json: 'application/json',
        pdf: 'application/pdf',
        xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }
      const blob = new Blob([response.data], { type: contentTypes[format] })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      const safeName = (project?.name ?? 'project').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')
      link.href = url
      link.download = `${safeName || 'project'}-v${version.versionNumber}.${format}`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to export the review package.'
      setError(message || 'Unable to export the review package.')
    } finally {
      setExportingFormat(null)
    }
  }

  const addRequirement = async (event: FormEvent) => {
    event.preventDefault()
    if (!projectId || !version) return

    try {
      const payload = {
        requirementId: requirementForm.requirementId,
        title: requirementForm.title,
        description: requirementForm.description,
        priority: requirementForm.priority,
        status: requirementForm.status,
      }
      await api.post(`/projects/${projectId}/analysis-versions/${version.id}/requirements`, payload, { headers: authHeader(auth.token) })
      await refreshReview(version.id)
      setRequirementForm({ ...requirementForm, requirementId: `REQ-${Math.floor(Math.random() * 900) + 100}`, title: '', description: '', priority: 'HIGH', status: 'PENDING_REVIEW' })
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to save the requirement.'
      setError(message || 'Unable to save the requirement.')
    }
  }

  const addQuestion = async (event: FormEvent) => {
    event.preventDefault()
    if (!projectId || !version) return

    try {
      await api.post(`/projects/${projectId}/analysis-versions/${version.id}/questions`, {
        questionId: questionForm.questionId,
        question: questionForm.question,
        relatedRequirementId: questionForm.relatedRequirementId || null,
        reason: questionForm.reason,
        impact: questionForm.impact,
        status: 'OPEN',
      }, { headers: authHeader(auth.token) })
      await refreshReview(version.id)
      setQuestionForm({ ...questionForm, questionId: `Q-${Math.floor(Math.random() * 900) + 100}`, question: '', relatedRequirementId: '', reason: '', impact: '' })
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to save the review question.'
      setError(message || 'Unable to save the review question.')
    }
  }

  const answerQuestion = async (questionId: string) => {
    if (!projectId || !version || !answerText.trim()) return

    try {
      await api.post(`/projects/${projectId}/analysis-versions/${version.id}/questions/${questionId}/answer`, { answer: answerText }, { headers: authHeader(auth.token) })
      setAnswerText('')
      await refreshReview(version.id)
    } catch (requestError: unknown) {
      const message = requestError && typeof requestError === 'object' && 'response' in requestError
        ? (requestError as { response?: { data?: { detail?: string; title?: string } } }).response?.data?.detail
        : 'Unable to answer the question.'
      setError(message || 'Unable to answer the question.')
    }
  }

  if (!project) {
    return (
      <main className="workspace shell">
        <p className="eyebrow">PROJECT</p>
        <section className="panel">
          {isLoadingProject ? (
            <p className="muted">Loading project…</p>
          ) : (
            <>
              <p className="form-error" role="alert">{error || 'Unable to load project data.'}</p>
              <Link to="/" className="ghost-button inline-link">Back to dashboard</Link>
            </>
          )}
        </section>
      </main>
    )
  }

  return (
    <main className="workspace shell">
      <header className="topbar app-topbar">
        <div className="wordmark" aria-label="Requirement Analyzer home">
          <span className="wordmark-mark" aria-hidden="true">RA</span>
          <span>Requirement Analyzer</span>
        </div>
        <div className="top-actions">
          <Link to="/" className="ghost-button inline-link">Dashboard</Link>
          <button type="button" className="ghost-button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <div className="project-header">
        <div>
          <p className="eyebrow">PROJECT</p>
          <h2>{project.name}</h2>
        </div>
        <span className="pill">{project.status}</span>
      </div>

      <section className="layout-two-column align-top">
        <div className="stacked-panel">
          <div className="panel">
            <p className="eyebrow">DOCUMENTS</p>
            <h3>Project files</h3>
            <form onSubmit={uploadDocument} className="stacked-form compact">
              <label>
                <span>Upload PDF, DOCX, TXT, or XLSX (up to 10 MB)</span>
                <input key={fileInputKey} name="document" type="file" accept=".pdf,.docx,.txt,.xlsx" />
              </label>
              <button type="submit" className="secondary-button" disabled={isUploading}>
                {isUploading ? 'Uploading...' : 'Upload document'}
              </button>
            </form>
            {documents.length === 0 ? (
              <p className="muted">No documents uploaded yet.</p>
            ) : (
              <ul className="list-block document-list">
                {documents.map((document) => (
                  <li key={document.id}>
                    <label className="document-choice">
                      <input
                        type="checkbox"
                        checked={selectedDocumentIds.includes(document.id)}
                        onChange={(event) => setSelectedDocumentIds((current) => (
                          event.target.checked
                            ? [...current, document.id]
                            : current.filter((id) => id !== document.id)
                        ))}
                      />
                      <span>
                        <strong>{document.fileName}</strong>
                        <small>{(document.fileSize / 1024).toFixed(1)} KB · {document.status}</small>
                      </span>
                    </label>
                    <button type="button" className="ghost-button" onClick={() => deleteDocument(document.id)}>
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="panel">
            <p className="eyebrow">ANALYZE</p>
            <h3>Submit new analysis</h3>
            <form onSubmit={submitAnalysis} className="stacked-form compact">
              <label>
                <span>Requirement text (optional when documents are selected)</span>
                <textarea value={text} onChange={(event) => setText(event.target.value)} rows={7} placeholder="Paste a user story, product brief, or requirement text here." />
              </label>
              <p className="muted">{selectedDocumentIds.length} document{selectedDocumentIds.length === 1 ? '' : 's'} selected</p>
              {error && <p className="form-error">{error}</p>}
              <button className="primary-button" type="submit" disabled={isSubmitting || (!text.trim() && selectedDocumentIds.length === 0)}>
                {isSubmitting ? 'Sending...' : 'Run analysis'}
              </button>
            </form>
          </div>

          <div className="panel">
            <p className="eyebrow">RECENT JOBS</p>
            <div className="job-list">
              {jobs.map((job) => (
                <button key={job.id} type="button" className={`job-item ${selectedJob?.id === job.id ? 'selected' : ''}`} onClick={() => setSelectedJob(job)}>
                  <span>{job.status}</span>
                  <small>{job.updatedAt.slice(0, 10)}</small>
                </button>
              ))}
            </div>
            {selectedJob && (
              <div className="selection-summary">
                <strong>{selectedJob.status}</strong>
                <p>{selectedJob.summary || selectedJob.errorMessage || 'Analysis is active.'}</p>
                {selectedJob.status === 'SUCCEEDED' && (
                  <>
                    <button type="button" className="secondary-button" onClick={createVersion}>Create review version</button>
                    {version && (
                      <div className="export-actions" aria-label="Export review version">
                        {(['json', 'pdf', 'xlsx'] as const).map((format) => (
                          <button
                            key={format}
                            type="button"
                            className="ghost-button"
                            onClick={() => exportVersion(format)}
                            disabled={exportingFormat !== null}
                          >
                            {exportingFormat === format ? 'Exporting...' : `Export ${format.toUpperCase()}`}
                          </button>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="panel review-panel">
          <p className="eyebrow">REVIEW</p>
          <h3>Human review workspace</h3>

          {!version && !review ? (
            <p className="muted">No review version has been created yet. Run a successful analysis and create a review draft.</p>
          ) : (
            <>
              <div className="version-card">
                <strong>{review?.version?.title ?? version?.title ?? 'Review version'}</strong>
                <p>{review?.version?.summary ?? version?.summary ?? 'Awaiting summary.'}</p>
              </div>

              <div className="review-section">
                <h4>Requirements</h4>
                <form onSubmit={addRequirement} className="stacked-form compact">
                  <div className="inline-grid">
                    <input value={requirementForm.requirementId} onChange={(event) => setRequirementForm((current) => ({ ...current, requirementId: event.target.value }))} placeholder="REQ-001" />
                    <input value={requirementForm.title} onChange={(event) => setRequirementForm((current) => ({ ...current, title: event.target.value }))} placeholder="Requirement title" required />
                  </div>
                  <textarea value={requirementForm.description} onChange={(event) => setRequirementForm((current) => ({ ...current, description: event.target.value }))} rows={3} placeholder="Requirement description" />
                  <div className="inline-grid">
                    <select value={requirementForm.priority} onChange={(event) => setRequirementForm((current) => ({ ...current, priority: event.target.value as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' }))}>
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="CRITICAL">Critical</option>
                    </select>
                    <select value={requirementForm.status} onChange={(event) => setRequirementForm((current) => ({ ...current, status: event.target.value as 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'MANUAL' }))}>
                      <option value="PENDING_REVIEW">Pending review</option>
                      <option value="APPROVED">Approved</option>
                      <option value="REJECTED">Rejected</option>
                      <option value="DRAFT">Draft</option>
                    </select>
                  </div>
                  <button type="submit" className="secondary-button">Add requirement</button>
                </form>

                <ul className="list-block">
                  {(review?.requirements ?? []).map((requirement) => (
                    <li key={requirement.id}>
                      <strong>{requirement.requirementId}</strong>
                      <span>{requirement.title}</span>
                      <small>{requirement.status} · {requirement.priority}</small>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="review-section">
                <h4>Questions</h4>
                <form onSubmit={addQuestion} className="stacked-form compact">
                  <div className="inline-grid">
                    <input value={questionForm.questionId} onChange={(event) => setQuestionForm((current) => ({ ...current, questionId: event.target.value }))} placeholder="Q-001" />
                    <input value={questionForm.relatedRequirementId} onChange={(event) => setQuestionForm((current) => ({ ...current, relatedRequirementId: event.target.value }))} placeholder="Related requirement" />
                  </div>
                  <textarea value={questionForm.question} onChange={(event) => setQuestionForm((current) => ({ ...current, question: event.target.value }))} rows={3} placeholder="Question for the team" required />
                  <div className="inline-grid">
                    <input value={questionForm.reason} onChange={(event) => setQuestionForm((current) => ({ ...current, reason: event.target.value }))} placeholder="Reason" />
                    <input value={questionForm.impact} onChange={(event) => setQuestionForm((current) => ({ ...current, impact: event.target.value }))} placeholder="Impact" />
                  </div>
                  <button type="submit" className="secondary-button">Add question</button>
                </form>

                <ul className="list-block">
                  {(review?.questions ?? []).map((question) => (
                    <li key={question.id}>
                      <strong>{question.questionId}</strong>
                      <span>{question.question}</span>
                      <small>{question.status}</small>
                      {question.status !== 'ANSWERED' && (
                        <div className="answer-row">
                          <input value={answerText} onChange={(event) => setAnswerText(event.target.value)} placeholder="Answer" />
                          <button type="button" className="secondary-button" onClick={() => answerQuestion(question.id)}>Answer</button>
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  )
}