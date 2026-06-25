import { describe, expect, it } from 'vitest'
import { agents, cases, decisions } from './mockData'

describe('POC fixtures', () => {
  it('keeps case identifiers unique', () => {
    expect(new Set(cases.map((item) => item.id)).size).toBe(cases.length)
  })

  it('links every decision to an existing case', () => {
    const caseIds = new Set(cases.map((item) => item.id))
    expect(decisions.every((item) => caseIds.has(item.caseId))).toBe(true)
  })

  it('does not exceed agent capacity', () => {
    expect(agents.every((agent) => agent.activeCases <= agent.capacity)).toBe(true)
  })
})
