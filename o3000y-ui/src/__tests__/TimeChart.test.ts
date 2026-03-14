import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TimeChart from '../components/TimeChart.vue'

const singleSeries = [
  {
    name: 'COUNT',
    data: [
      { bucket: '2026-03-14T10:00:00', value: 10 },
      { bucket: '2026-03-14T10:05:00', value: 25 },
      { bucket: '2026-03-14T10:10:00', value: 15 },
      { bucket: '2026-03-14T10:15:00', value: 30 },
    ],
    color: 'var(--color-chart-1)',
  },
]

describe('TimeChart', () => {
  it('renders SVG with data points', () => {
    const wrapper = mount(TimeChart, { props: { series: singleSeries, label: 'COUNT' } })
    expect(wrapper.find('svg').exists()).toBe(true)
  })

  it('shows empty state when no data', () => {
    const wrapper = mount(TimeChart, { props: { series: [], label: 'COUNT' } })
    expect(wrapper.find('svg').exists()).toBe(false)
    expect(wrapper.text()).toContain('No data')
  })

  it('displays label', () => {
    const wrapper = mount(TimeChart, { props: { series: singleSeries, label: 'P99(duration_us)' } })
    expect(wrapper.text()).toContain('P99(duration_us)')
  })

  it('renders legend for multi-series', () => {
    const multi = [
      { name: 'api-gateway', data: [{ bucket: '2026-03-14T10:00:00', value: 10 }], color: 'red' },
      { name: 'user-service', data: [{ bucket: '2026-03-14T10:00:00', value: 5 }], color: 'blue' },
    ]
    const wrapper = mount(TimeChart, { props: { series: multi, label: 'COUNT' } })
    expect(wrapper.text()).toContain('api-gateway')
    expect(wrapper.text()).toContain('user-service')
  })
})
