import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import ResultsTable from '../components/ResultsTable.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }],
})

describe('ResultsTable', () => {
  it('renders columns and rows with numeric alignment', () => {
    const wrapper = mount(ResultsTable, {
      global: { plugins: [router] },
      props: {
        result: {
          columns: ['name', 'value'],
          rows: [
            ['foo', 42],
            ['bar', 99],
          ],
          rowCount: 2,
          elapsedMs: 10,
        },
      },
    })

    expect(wrapper.text()).toContain('name')
    expect(wrapper.text()).toContain('value')
    expect(wrapper.text()).toContain('foo')
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('2 rows')

    // Numeric column should have right-aligned class
    const ths = wrapper.findAll('th')
    expect(ths[1].classes()).toContain('th--numeric')
  })

  it('renders trace_id as clickable cells', () => {
    const wrapper = mount(ResultsTable, {
      global: { plugins: [router] },
      props: {
        result: {
          columns: ['trace_id', 'span_id'],
          rows: [['abc123', 'span1']],
          rowCount: 1,
          elapsedMs: 5,
        },
      },
    })

    const cells = wrapper.findAll('td')
    const traceCell = cells[0]
    expect(traceCell.classes()).toContain('td--link')
    expect(traceCell.text()).toBe('abc123')
  })

  it('highlights error rows', () => {
    const wrapper = mount(ResultsTable, {
      global: { plugins: [router] },
      props: {
        result: {
          columns: ['service_name', 'status_code'],
          rows: [
            ['api-gateway', 1],
            ['payment-svc', 2],
          ],
          rowCount: 2,
          elapsedMs: 5,
        },
      },
    })

    const rows = wrapper.findAll('tbody tr')
    expect(rows[0].classes()).not.toContain('row--error')
    expect(rows[1].classes()).toContain('row--error')
  })
})
