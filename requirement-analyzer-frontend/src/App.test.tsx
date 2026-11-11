import '@testing-library/jest-dom/vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { api } from './lib/api'

vi.mock('./lib/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('App', () => {
  const project = {
    id: 'project-1',
    name: 'Attendance platform',
    ownerId: 'owner-1',
    status: 'ACTIVE',
    createdAt: '2026-11-11T00:00:00Z',
    updatedAt: '2026-11-11T00:00:00Z',
  }

  const document = {
    id: 'document-1',
    projectId: project.id,
    fileName: 'requirements.txt',
    contentType: 'text/plain',
    fileSize: 1024,
    status: 'UPLOADED',
    createdAt: '2026-11-11T00:00:00Z',
  }

  const auth = {
    token: 'test-token',
    expiresAt: '2026-11-12T00:00:00Z',
    user: { id: 'owner-1', email: 'owner@example.com', displayName: 'Project Owner', status: 'ACTIVE' },
  }

  const signInState = () => {
    localStorage.setItem('requirement-analyzer-auth', JSON.stringify(auth))
  }

  beforeEach(() => {
    localStorage.clear()
    window.history.replaceState({}, '', '/')
    vi.mocked(api.get).mockImplementation(async (url) => {
      if (url === '/health') return { data: { status: 'UP', service: 'requirement-analyzer-backend' } }
      if (url === '/projects') return { data: { content: [project] } }
      if (url === `/projects/${project.id}`) return { data: project }
      if (url === `/projects/${project.id}/analyses`) return { data: { content: [] } }
      if (url === `/projects/${project.id}/documents`) return { data: { content: [] } }
      return { data: {} }
    })
  })

  it('shows the sign-in entry point for the requirement workspace', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /requirement analyzer/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /sign in/i }).length).toBeGreaterThan(0)
  })

  it('uploads and selects a project document for analysis', async () => {
    signInState()
    vi.mocked(api.post)
      .mockResolvedValueOnce({ data: document })
      .mockResolvedValueOnce({ data: {
        id: 'job-1',
        projectId: project.id,
        ownerId: 'owner-1',
        text: '',
        documentIds: [document.id],
        status: 'QUEUED',
        createdAt: '2026-11-11T00:00:00Z',
        updatedAt: '2026-11-11T00:00:00Z',
      } })
    vi.mocked(api.delete).mockResolvedValue({ data: undefined })

    render(<App />)
    fireEvent.click(await screen.findByRole('link', { name: /attendance platform/i }))

    const file = new File(['requirements'], 'requirements.txt', { type: 'text/plain' })
    fireEvent.change(await screen.findByLabelText(/upload pdf, docx, txt, or xlsx/i), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'Upload document' }))

    const documentChoice = await screen.findByRole('checkbox', { name: /requirements\.txt/i })
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      `/projects/${project.id}/documents`,
      expect.any(FormData),
      { headers: { Authorization: 'Bearer test-token' } },
    ))
    fireEvent.click(documentChoice)
    fireEvent.click(screen.getByRole('button', { name: 'Run analysis' }))

    await waitFor(() => expect(api.post).toHaveBeenLastCalledWith(
      `/projects/${project.id}/analyses`,
      { text: '', documentIds: [document.id] },
      { headers: { Authorization: 'Bearer test-token' } },
    ))
    fireEvent.click(screen.getByRole('button', { name: 'Remove' }))

    await waitFor(() => expect(api.delete).toHaveBeenCalledWith(
      `/documents/${document.id}`,
      { headers: { Authorization: 'Bearer test-token' } },
    ))
    expect(screen.queryByRole('checkbox', { name: /requirements\.txt/i })).not.toBeInTheDocument()
    expect(screen.getByText('0 documents selected')).toBeInTheDocument()
  })

  it('creates a review version and requests PDF and Excel downloads', async () => {
    signInState()
    const job = {
      id: 'job-1',
      projectId: project.id,
      ownerId: 'owner-1',
      text: 'Users can view attendance reports.',
      documentIds: [],
      status: 'SUCCEEDED',
      summary: 'Attendance report requirements.',
      createdAt: '2026-11-11T00:00:00Z',
      updatedAt: '2026-11-11T00:00:00Z',
    }
    const version = {
      id: 'version-1',
      projectId: project.id,
      analysisJobId: job.id,
      versionNumber: 1,
      title: 'Review draft',
      status: 'DRAFT',
      createdAt: '2026-11-11T00:00:00Z',
      updatedAt: '2026-11-11T00:00:00Z',
    }
    vi.mocked(api.get).mockImplementation(async (url) => {
      if (url === '/health') return { data: { status: 'UP', service: 'requirement-analyzer-backend' } }
      if (url === '/projects') return { data: { content: [project] } }
      if (url === `/projects/${project.id}`) return { data: project }
      if (url === `/projects/${project.id}/analyses`) return { data: { content: [job] } }
      if (url === `/projects/${project.id}/documents`) return { data: { content: [] } }
      if (url === `/projects/${project.id}/analysis-versions/${version.id}/review`) {
        return { data: { version, requirements: [], questions: [], auditEvents: [] } }
      }
      if (url.includes('/export/')) return { data: new Blob(['export']) }
      return { data: {} }
    })
    vi.mocked(api.post).mockResolvedValue({ data: version })
    const createObjectURL = vi.fn(() => 'blob:export')
    const revokeObjectURL = vi.fn()
    const clickLink = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})

    render(<App />)
    fireEvent.click(await screen.findByRole('link', { name: /attendance platform/i }))
    fireEvent.click(await screen.findByRole('button', { name: 'Create review version' }))
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(
      `/projects/${project.id}/analysis-versions/${version.id}/review`,
      expect.any(Object),
    ))

    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })
    fireEvent.click(await screen.findByRole('button', { name: 'Export PDF' }))
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(
      `/projects/${project.id}/analysis-versions/${version.id}/export/pdf`,
      expect.objectContaining({ responseType: 'blob' }),
    ))
    fireEvent.click(screen.getByRole('button', { name: 'Export XLSX' }))
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(
      `/projects/${project.id}/analysis-versions/${version.id}/export/xlsx`,
      expect.objectContaining({ responseType: 'blob' }),
    ))
    expect(createObjectURL).toHaveBeenCalledTimes(2)
    clickLink.mockRestore()
    vi.unstubAllGlobals()
  })
})