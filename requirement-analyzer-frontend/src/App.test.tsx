import '@testing-library/jest-dom/vitest'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { api } from './lib/api'

vi.mock('./lib/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('App', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockResolvedValue({
      data: { status: 'UP', service: 'requirement-analyzer-backend' },
    })
  })

  it('shows the sign-in entry point for the requirement workspace', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /requirement analyzer/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /sign in/i }).length).toBeGreaterThan(0)
  })
})