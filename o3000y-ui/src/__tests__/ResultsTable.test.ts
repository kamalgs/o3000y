import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import ResultsTable from '../components/ResultsTable.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }],
})

describe('ResultsTable', () => {
  it('renders columns and rows', () => {
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
    expect(wrapper.text()).toContain('2 rows in 10ms')
  })

  it('renders trace_id as links', () => {
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

    const link = wrapper.find('a')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe('abc123')
  })
})
