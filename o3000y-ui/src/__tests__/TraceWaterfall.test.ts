import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TraceWaterfall from '../components/TraceWaterfall.vue'
import type { TraceResponse } from '../api/client'

const sampleTrace: TraceResponse = {
  traceId: 'abc123',
  spanCount: 3,
  spans: [
    {
      traceId: 'abc123',
      spanId: 'span1',
      parentSpanId: '',
      operationName: 'root-op',
      serviceName: 'frontend',
      startTime: '2026-02-09T14:30:00Z',
      endTime: '2026-02-09T14:30:01Z',
      durationUs: 1000000,
      statusCode: 1,
      statusMessage: '',
      spanKind: 2,
      attributes: { 'http.method': 'GET' },
    },
    {
      traceId: 'abc123',
      spanId: 'span2',
      parentSpanId: 'span1',
      operationName: 'child-op-1',
      serviceName: 'backend',
      startTime: '2026-02-09T14:30:00.100Z',
      endTime: '2026-02-09T14:30:00.500Z',
      durationUs: 400000,
      statusCode: 1,
      statusMessage: '',
      spanKind: 2,
      attributes: {},
    },
    {
      traceId: 'abc123',
      spanId: 'span3',
      parentSpanId: 'span1',
      operationName: 'child-op-2',
      serviceName: 'database',
      startTime: '2026-02-09T14:30:00.500Z',
      endTime: '2026-02-09T14:30:00.800Z',
      durationUs: 300000,
      statusCode: 1,
      statusMessage: '',
      spanKind: 3,
      attributes: {},
    },
  ],
}

describe('TraceWaterfall', () => {
  it('renders all spans', () => {
    const wrapper = mount(TraceWaterfall, {
      props: { trace: sampleTrace },
    })

    expect(wrapper.text()).toContain('3 spans')
    expect(wrapper.text()).toContain('root-op')
    expect(wrapper.text()).toContain('child-op-1')
    expect(wrapper.text()).toContain('child-op-2')
  })

  it('renders service color legend', () => {
    const wrapper = mount(TraceWaterfall, {
      props: { trace: sampleTrace },
    })

    expect(wrapper.text()).toContain('frontend')
    expect(wrapper.text()).toContain('backend')
    expect(wrapper.text()).toContain('database')
  })

  it('shows span details on click', async () => {
    const wrapper = mount(TraceWaterfall, {
      props: { trace: sampleTrace },
    })

    // Click on first span row
    const rows = wrapper.findAll('[class*="hover:bg-gray-50"]')
    expect(rows.length).toBe(3)

    await rows[0].trigger('click')
    expect(wrapper.text()).toContain('Span Details')
    expect(wrapper.text()).toContain('root-op')
    expect(wrapper.text()).toContain('http.method')
    expect(wrapper.text()).toContain('GET')
  })
})
