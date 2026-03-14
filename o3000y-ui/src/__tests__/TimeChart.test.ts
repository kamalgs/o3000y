import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TimeChart from '../components/TimeChart.vue'

describe('TimeChart', () => {
  const sampleData = [
    { bucket: '2026-03-14T10:00:00', value: 10 },
    { bucket: '2026-03-14T10:05:00', value: 25 },
    { bucket: '2026-03-14T10:10:00', value: 15 },
    { bucket: '2026-03-14T10:15:00', value: 30 },
  ]

  it('renders SVG with data points', () => {
    const wrapper = mount(TimeChart, {
      props: { data: sampleData, label: 'COUNT' },
    })
    expect(wrapper.find('svg').exists()).toBe(true)
    // 4 data points = 4 circles
    expect(wrapper.findAll('circle').length).toBe(4)
  })

  it('shows empty state when no data', () => {
    const wrapper = mount(TimeChart, {
      props: { data: [], label: 'COUNT' },
    })
    expect(wrapper.find('svg').exists()).toBe(false)
    expect(wrapper.text()).toContain('No data')
  })

  it('displays label', () => {
    const wrapper = mount(TimeChart, {
      props: { data: sampleData, label: 'P99(duration_us)' },
    })
    expect(wrapper.text()).toContain('P99(duration_us)')
  })
})
