'use strict'

const test = require('node:test')
const assert = require('node:assert/strict')
const babel = require('@babel/core')
const plugin = require('../dist/index.js')

function transform(code, options) {
  return babel.transformSync(code, {
    filename: 'test.tsx',
    parserOpts: { plugins: ['jsx', 'typescript'] },
    plugins: [[plugin, options]],
    configFile: false,
    babelrc: false,
  }).code
}

const IMPORT_RE = /import \{ Track \} from ['"]@aquarush\/common\/tracking['"];?/
const ANY_IMPORT_RE = /import \{ Track \} from ['"][^'"]+['"];?/

test('基础包裹 + import 注入', () => {
  const out = transform('<Button trackTarget="add-cart" />')
  assert.match(out, /<Track target="add-cart">/)
  assert.match(out, /<Button \/>/)
  assert.match(out, IMPORT_RE)
  assert.doesNotMatch(out, /trackTarget/)
})

test('完整属性映射', () => {
  const out = transform(
    '<Button trackTarget="login" trackAction="FORM_SUBMIT" trackElementId="btn-login" trackElementType="button" trackElementText="登录" />',
  )
  assert.match(
    out,
    /<Track action="FORM_SUBMIT" target="login" elementId="btn-login" elementType="button" elementText="登录">/,
  )
})

test('trackProps 对象 + 散列键合并', () => {
  const out = transform('<Button trackTarget="add-cart" trackProps={{ productId: id }} trackOrderNo="A001" />')
  assert.match(out, /props=\{\{\s*productId: id,\s*orderNo: "A001"\s*\}\}/)
})

test('trackProps JSON 字符串解析', () => {
  const out = transform('<Button trackTarget="add-cart" trackProps=\'{"a":1}\' />')
  assert.match(out, /props=\{\{\s*a: 1\s*\}\}/)
})

test('trackProps 其他表达式 + 散列键', () => {
  const out = transform('<Button trackTarget="x" trackProps={ctx} trackFrom="cart" />')
  assert.match(out, /props=\{\{\s*\.\.\.ctx,\s*from: "cart"\s*\}\}/)
})

test('已有 onClick 保留在子元素', () => {
  const out = transform('<Button trackTarget="pay" onClick={handle}>支付</Button>')
  assert.match(out, /<Track target="pay">/)
  assert.match(out, /<Button onClick=\{handle\}>支付<\/Button>/)
})

test('trackDisabled → disabled 动态跳过', () => {
  const out = transform('<Button trackTarget="x" trackDisabled={stock <= 0} onClick={h} />')
  assert.match(out, /<Track target="x" disabled=\{stock <= 0\}>/)
  assert.match(out, /<Button onClick=\{h\} \/>/)
})

test('trackIgnore 为真 → 剥离不包裹', () => {
  const out = transform('<Button trackTarget="x" trackIgnore="true" onClick={h} />')
  assert.doesNotMatch(out, /<Track/)
  assert.doesNotMatch(out, /trackTarget/)
  assert.match(out, /<Button onClick=\{h\} \/>/)
  assert.doesNotMatch(out, IMPORT_RE)
})

test('trackIgnore="false" → 正常包裹', () => {
  const out = transform('<Button trackTarget="x" trackIgnore="false" onClick={h} />')
  assert.match(out, /<Track target="x">/)
  assert.doesNotMatch(out, /trackIgnore/)
})

test('trackIgnore 裸属性 → 剥离不包裹', () => {
  const out = transform('<Button trackTarget="x" trackIgnore onClick={h} />')
  assert.doesNotMatch(out, /<Track/)
  assert.doesNotMatch(out, /trackTarget/)
})

test('缺 trackTarget → 跳过并告警(不转换)', () => {
  const warns = []
  const origWarn = console.warn
  console.warn = (msg) => warns.push(msg)
  try {
    const out = transform('<Button trackElementText="x" onClick={h} />')
    assert.equal(out, '<Button trackElementText="x" onClick={h} />;')
    assert.equal(warns.length, 1)
    assert.match(warns[0], /trackTarget is required/)
  } finally {
    console.warn = origWarn
  }
})

test('key 移到外层、ref 留在内层', () => {
  const out = transform('<li trackTarget="item" key={k} ref={r}>x</li>')
  assert.match(out, /<Track (?:key=\{k\} )?target="item"(?: key=\{k\})?>/)
  assert.match(out, /<li ref=\{r\}>x<\/li>/)
})

test('已有同源 import 不重复注入', () => {
  const out = transform(
    "import { Track } from '@aquarush/common/tracking'\n<Button trackTarget=\"x\" />",
  )
  assert.equal(out.match(IMPORT_RE).length, 1)
})

test('Track 标识符冲突 → 生成唯一名', () => {
  const out = transform('const Track = 1;\n<Button trackTarget="x" />')
  assert.match(out, /import \{ Track as Track\$1 \} from ['"]@aquarush\/common\/tracking['"]/)
  assert.match(out, /<Track\$1 target="x">/)
})

test('多元素一文件 → 全部包裹、仅一个 import', () => {
  const out = transform('<><A trackTarget="a" /><B trackTarget="b" /></>')
  assert.match(out, /<Track target="a">/)
  assert.match(out, /<Track target="b">/)
  assert.equal(out.match(IMPORT_RE).length, 1)
})

test('嵌套元素各自包裹', () => {
  const out = transform('<View><Button trackTarget="inner" /></View>')
  assert.match(out, /<View><Track target="inner"><Button \/><\/Track><\/View>/)
})

test('kebab 形式 track-target', () => {
  const out = transform('<Button track-target="add-cart" />')
  assert.match(out, /<Track target="add-cart">/)
  assert.match(out, /<Button \/>/)
})

test('无 track-* 文件零改动', () => {
  const src = '<Button onClick={h}>x</Button>'
  const out = transform(src)
  assert.equal(out, src + ';')
  assert.doesNotMatch(out, ANY_IMPORT_RE)
})

test('moduleSource 选项生效', () => {
  const out = transform('<Button trackTarget="x" />', { moduleSource: './components/Track' })
  assert.match(out, /import \{ Track \} from ['"]\.\/components\/Track['"]/)
})

test('自闭合元素包裹', () => {
  const out = transform('<Button trackTarget="x" />')
  assert.match(out, /<Track target="x"><Button \/><\/Track>/)
})

test('子元素原有 children 保留', () => {
  const out = transform('<Button trackTarget="x" onClick={h}>去支付 <Icon /></Button>')
  assert.match(out, /<Button onClick=\{h\}>去支付 <Icon \/><\/Button>/)
})

test('JSXSpreadAttribute 保留在内层元素', () => {
  const out = transform('<Button trackTarget="x" {...rest} onClick={h} />')
  assert.match(out, /<Track target="x">/)
  assert.match(out, /<Button .*\{\s*\.\.\.rest\s*\}.* \/>/)
  assert.match(out, /onClick=\{h\}/)
})

test('trackIgnore 剥离时保留 spread 属性', () => {
  const out = transform('<Button trackTarget="x" trackIgnore="true" {...rest} onClick={h} />')
  assert.doesNotMatch(out, /<Track/)
  assert.match(out, /<Button \{\.\.\.rest\} onClick=\{h\} \/>/)
})
