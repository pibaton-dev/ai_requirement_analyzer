import '@testing-library/jest-dom/vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { api } from './lib/api'

vi.mock('./lib/api', () => ({
  api: { get: vi.fn() },
}))

describe('App', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockResolvedValue({
      data: { status: 'UP', service: 'requirement-analyzer-backend' },
    })
  })

  it('shows the workspace and API health', async () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /turn project intent/i })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Online'))
  })
})