/**
 * Form DOM 回归(spec admin-radix-ui CRITICAL-1):
 * Form.Item 注入的 onChange 必须「先写表单值,再回调子组件自身 onChange」
 * (antd rc-field-form getControlled 同语义)—— 子组件在自己的 onChange 里
 * 读 form.getFieldsValue() 应已看到新值。
 */
import { afterEach, describe, expect, it, jest } from '@jest/globals'
import { useEffect } from 'react'
import { Form } from './Form'
import { useForm, type FormInstance } from './useForm'
import { changeInput, cleanup, render } from './test-utils'

interface RegionValues {
  region?: string
}

afterEach(cleanup)

describe('Form DOM(CRITICAL-1:注入 onChange 先表单后子组件)', () => {
  it('子组件自身 onChange 被回调,且回调时表单值已是新值(表单先、子组件后)', () => {
    // 子组件自身 onChange:记录回调时点的表单值与调用次数
    const valuesAtOwnCall: unknown[] = []
    const ownHandler = jest.fn((event: unknown) => {
      valuesAtOwnCall.push(formInstance?.getFieldsValue())
      void event
    })

    let formInstance: FormInstance<RegionValues> | undefined

    function Harness(): React.ReactElement {
      const [form] = useForm<RegionValues>()
      // 实例捕获放 effect(render 期写外层变量违反 react-hooks 纯渲染规则);
      // render() 返回时 effect 已 flush,后续交互读到的必是已赋值实例
      useEffect(() => {
        formInstance = form
      }, [form])
      return (
        // Form.Item initialValue 直达 Controller defaultValue:input 首帧即受控,
        // 避免 undefined→'值' 的 uncontrolled→controlled React 警告
        <Form form={form}>
          <Form.Item name="region" initialValue="">
            <input onChange={ownHandler} />
          </Form.Item>
        </Form>
      )
    }

    const { container } = render(<Harness />)
    const input = container.querySelector('input')
    expect(input).not.toBeNull()

    changeInput(input!, '华东')

    // 子组件自身 handler 被调用一次,入参为携带新值的事件
    expect(ownHandler).toHaveBeenCalledTimes(1)
    const eventArg = ownHandler.mock.calls[0][0] as Event
    expect(['input', 'change']).toContain(eventArg.type)
    expect((eventArg.target as HTMLInputElement).value).toBe('华东')

    // 表单值已写入
    expect(formInstance?.getFieldsValue()).toEqual({ region: '华东' })

    // 关键顺序断言:own handler 触发时读到的表单值已是新值(表单先、子组件后)
    expect(valuesAtOwnCall).toEqual([{ region: '华东' }])

    // 输入框显示新值(受控回填)
    expect((input as HTMLInputElement).value).toBe('华东')
  })
})
