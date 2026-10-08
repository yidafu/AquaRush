/**
 * debounce 工具单测
 */
import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals'
import { debounce } from './debounce'

describe('debounce', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('在等待窗口内合并多次调用,只执行最后一次', () => {
    const fn = jest.fn()
    const debounced = debounce(fn, 300)

    debounced(1)
    debounced(2)
    debounced(3)

    expect(fn).not.toHaveBeenCalled()
    jest.advanceTimersByTime(299)
    expect(fn).not.toHaveBeenCalled()
    jest.advanceTimersByTime(1)
    expect(fn).toHaveBeenCalledTimes(1)
    expect(fn).toHaveBeenCalledWith(3)
  })

  it('窗口结束后再次调用会重新开始计时', () => {
    const fn = jest.fn()
    const debounced = debounce(fn, 100)

    debounced('a')
    jest.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(1)

    debounced('b')
    jest.advanceTimersByTime(50)
    debounced('c')
    jest.advanceTimersByTime(100)
    expect(fn).toHaveBeenCalledTimes(2)
    expect(fn).toHaveBeenLastCalledWith('c')
  })

  it('透传所有参数', () => {
    const fn = jest.fn()
    const debounced = debounce(fn, 50)

    debounced('x', 1, { k: true })
    jest.advanceTimersByTime(50)

    expect(fn).toHaveBeenCalledWith('x', 1, { k: true })
  })
})
