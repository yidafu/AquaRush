/**
 * Table DOM 行为测试(spec admin-radix-ui T7 / S2.9):
 * - onRow 行点击回调;
 * - ellipsis 列截断样式 + title 全文;
 * - scroll.x 横向滚动容器;
 * - fixed 列 sticky 定位;
 * - CRITICAL-2 回归:非受控分页点击翻页 vs 受控分页回调不改行。
 */
import { afterEach, describe, expect, it, jest } from '@jest/globals'
import type { HTMLAttributes } from 'react'
import { Table, type Column } from './Table'
import { cleanup, click, render } from './test-utils'

interface Row {
  id: number
  name: string
  desc: string
}

function makeRows(count: number): Row[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    name: `row ${i + 1}`,
    desc: i === 0 ? '一段足够长的描述文本用于 ellipsis 截断断言' : `desc ${i + 1}`,
  }))
}

const nameColumn: Column<Row> = { title: '名称', dataIndex: 'name', key: 'name' }

afterEach(cleanup)

describe('Table DOM(onRow / ellipsis / scroll / fixed)', () => {
  it('onRow 返回的 onClick 挂在行上,点击行触发,onRow 以 (record, index) 调用', () => {
    const rows = makeRows(3)
    const rowClick = jest.fn()
    const onRow = jest.fn((record: Row, index: number): HTMLAttributes<HTMLElement> => {
      void record
      void index
      return { onClick: rowClick }
    })
    const { container } = render(
      <Table columns={[nameColumn]} dataSource={rows} rowKey="id" pagination={false} onRow={onRow} />,
    )

    expect(onRow).toHaveBeenCalledTimes(3)
    expect(onRow).toHaveBeenNthCalledWith(1, rows[0], 0)
    expect(onRow).toHaveBeenNthCalledWith(3, rows[2], 2)

    const firstRow = container.querySelector('tbody tr')
    expect(firstRow).not.toBeNull()
    expect(rowClick).not.toHaveBeenCalled()
    click(firstRow!)
    expect(rowClick).toHaveBeenCalledTimes(1)
  })

  it('ellipsis: true 列的内层携带截断样式,td 带 title 全文', () => {
    const rows = makeRows(1)
    const columns: Column<Row>[] = [nameColumn, { title: '描述', dataIndex: 'desc', key: 'desc', ellipsis: true }]
    const { container } = render(
      <Table columns={columns} dataSource={rows} rowKey="id" pagination={false} />,
    )

    const tds = container.querySelectorAll('tbody tr td')
    expect(tds).toHaveLength(2)
    const ellipsisTd = tds[1]
    // title = 单元格全文
    expect(ellipsisTd.getAttribute('title')).toBe(rows[0].desc)
    // 内层 div 携带截断样式(overflow hidden / text-overflow ellipsis / nowrap)
    const inner = ellipsisTd.firstElementChild as HTMLElement
    expect(inner).not.toBeNull()
    expect(inner.tagName).toBe('DIV')
    const computed = getComputedStyle(inner)
    expect(computed.overflow).toBe('hidden')
    expect(computed.textOverflow).toBe('ellipsis')
    expect(computed.whiteSpace).toBe('nowrap')

    // 非 ellipsis 列不带 title
    expect(tds[0].getAttribute('title')).toBeNull()
  })

  it('scroll={{ x: 1200 }} 产生横向滚动容器(overflow-x: auto)与表格 min-width', () => {
    const rows = makeRows(2)
    const { container } = render(
      <Table columns={[nameColumn]} dataSource={rows} rowKey="id" pagination={false} scroll={{ x: 1200 }} />,
    )

    const table = container.querySelector('table') as HTMLTableElement
    const scrollContainer = table.parentElement as HTMLElement
    expect(getComputedStyle(scrollContainer).overflowX).toBe('auto')
    // scroll.x 注入表格内联宽度,min-width 保证短表不塌
    expect(table.style.width).toBe('1200px')
    expect(table.style.minWidth).toBe('100%')
  })

  it("fixed: 'left' | 'right' 列的 th/td 为 sticky 定位并带 offset", () => {
    const rows = makeRows(2)
    const columns: Column<Row>[] = [
      { title: '名称', dataIndex: 'name', key: 'name', fixed: 'left', width: 100 },
      { title: '描述', dataIndex: 'desc', key: 'desc' },
      { title: '操作', dataIndex: 'name', key: 'op', fixed: 'right', width: 80 },
    ]
    const { container } = render(
      <Table columns={columns} dataSource={rows} rowKey="id" pagination={false} scroll={{ x: 1200 }} />,
    )

    const ths = [...container.querySelectorAll('thead th')].map((el) => el as HTMLElement)
    const tds = [...container.querySelectorAll('tbody tr:first-child td')].map((el) => el as HTMLElement)
    expect(ths).toHaveLength(3)
    expect(tds).toHaveLength(3)

    // 左固定:sticky + left offset(首列无选择列时 offset 0)
    expect(getComputedStyle(ths[0]).position).toBe('sticky')
    expect(ths[0].style.left).toBe('0px')
    expect(getComputedStyle(tds[0]).position).toBe('sticky')
    expect(tds[0].style.left).toBe('0px')

    // 右固定:sticky + right offset
    expect(getComputedStyle(ths[2]).position).toBe('sticky')
    expect(ths[2].style.right).toBe('0px')
    expect(getComputedStyle(tds[2]).position).toBe('sticky')
    expect(tds[2].style.right).toBe('0px')

    // 中列不 sticky
    expect(getComputedStyle(ths[1]).position).not.toBe('sticky')
    expect(tds[1].style.left).toBe('')
    expect(tds[1].style.right).toBe('')
  })
})

describe('Table DOM(CRITICAL-2:非受控/受控分页)', () => {
  const columns: Column<Row>[] = [nameColumn]

  function pageButton(container: HTMLElement, page: number): HTMLButtonElement {
    const buttons = [...container.querySelectorAll('button')]
    const target = buttons.find(
      (b) => b.textContent === String(page) && b.getAttribute('aria-label') === null,
    )
    expect(target).toBeDefined()
    return target as HTMLButtonElement
  }

  it('非受控分页(pageSize 10 / total 30,无 current/onChange):点击第 2 页渲染 11+', () => {
    const rows = makeRows(30)
    const { container } = render(
      <Table columns={columns} dataSource={rows} rowKey="id" pagination={{ pageSize: 10, total: 30 }} />,
    )

    let trs = container.querySelectorAll('tbody tr')
    expect(trs).toHaveLength(10)
    expect(trs[0].textContent).toContain('row 1')
    expect(container.textContent).not.toContain('row 11')

    click(pageButton(container, 2))

    trs = container.querySelectorAll('tbody tr')
    expect(trs).toHaveLength(10)
    expect(trs[0].textContent).toContain('row 11')
    expect(trs[9].textContent).toContain('row 20')
    // 第 1 页行不再渲染(精确匹配整格文本,'row 1' 是 'row 11' 的子串不能用 toContain)
    const cellTexts = [...container.querySelectorAll('tbody td')].map((td) => td.textContent?.trim())
    expect(cellTexts).not.toContain('row 1')
  })

  it('受控分页(current=1):点击第 2 页触发 onChange,行不变直到 prop 变化', () => {
    const rows = makeRows(30)
    const paginationOnChange = jest.fn()
    const tableOnChange = jest.fn()
    const basePagination = {
      pageSize: 10,
      total: 30,
      onChange: paginationOnChange,
    } as const

    const view = render(
      <Table
        columns={columns}
        dataSource={rows}
        rowKey="id"
        pagination={{ current: 1, ...basePagination }}
        onChange={tableOnChange}
      />,
    )

    let trs = view.container.querySelectorAll('tbody tr')
    expect(trs).toHaveLength(10)
    expect(trs[0].textContent).toContain('row 1')

    click(pageButton(view.container, 2))

    // 回调触发(pagination.onChange + Table onChange),但受控 current=1 → 行不变
    expect(paginationOnChange).toHaveBeenCalledWith(2, 10)
    expect(tableOnChange).toHaveBeenCalledTimes(1)
    expect(tableOnChange.mock.calls[0][0]).toMatchObject({ current: 2, pageSize: 10 })
    expect(tableOnChange.mock.calls[0][3]).toMatchObject({ action: 'paginate' })
    trs = view.container.querySelectorAll('tbody tr')
    expect(trs).toHaveLength(10)
    expect(trs[0].textContent).toContain('row 1')
    expect(view.container.textContent).not.toContain('row 11')

    // prop.current 变为 2 后行才更新
    view.rerender(
      <Table
        columns={columns}
        dataSource={rows}
        rowKey="id"
        pagination={{ current: 2, ...basePagination }}
        onChange={tableOnChange}
      />,
    )
    trs = view.container.querySelectorAll('tbody tr')
    expect(trs).toHaveLength(10)
    expect(trs[0].textContent).toContain('row 11')
    expect(trs[9].textContent).toContain('row 20')
  })
})
