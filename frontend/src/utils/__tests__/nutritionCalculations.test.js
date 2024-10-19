import { describe, it, expect } from 'vitest'
import { calculateKcal } from '../nutritionCalculations'

describe('calculateKcal', () => {
  it('calculates calories correctly', () => {
    expect(calculateKcal(10, 20, 5)).toBe(165)
    expect(calculateKcal(0, 0, 0)).toBe(0)
    expect(calculateKcal(5, 10, 15)).toBe(195)
  })
})
