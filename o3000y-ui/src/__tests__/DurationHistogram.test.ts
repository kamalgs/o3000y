import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DurationHistogram from '../components/DurationHistogram.vue'
import type { QueryResponse } from '../api/client'

describe('DurationHistogram', () => {
  it('renders histogram for results with duration_us column', () => {
    const result: QueryResponse = {
      columns: ['trace_id', 'duration_us'],
      rows: [
        ['t1', 1000],
        ['t2', 2000],
        ['t3', 5000],
        ['t4', 10000],
        ['t5', 50000],
      ],
      rowCount: 5,
      elapsedMs: 10,
    }

    const wrapper = mount(DurationHistogram, { props: { result } })
    expect(wrapper.text()).toContain('Duration Distribution')
  })

  it('does not render when no duration_us column', () => {
    const result: QueryResponse = {
      columns: ['trace_id', 'span_id'],
      rows: [['t1', 's1']],
      rowCount: 1,
      elapsedMs: 5,
    }

    const wrapper = mount(DurationHistogram, { props: { result } })
    expect(wrapper.text()).not.toContain('Duration Distribution')
  })

  it('does not render when all durations are the same', () => {
    const result: QueryResponse = {
      columns: ['duration_us'],
      rows: [[1000], [1000], [1000]],
      rowCount: 3,
      elapsedMs: 5,
    }

    const wrapper = mount(DurationHistogram, { props: { result } })
    expect(wrapper.text()).not.toContain('Duration Distribution')
  })
})
