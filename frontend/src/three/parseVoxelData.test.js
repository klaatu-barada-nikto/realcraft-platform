import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseVoxelData } from './parseVoxelData.js'

test('空数组返回空结果', () => {
  assert.deepEqual(parseVoxelData([]), { blocks: [], count: 0, invalid: 0 })
})

test('非数组输入返回空结果', () => {
  assert.deepEqual(parseVoxelData(null), { blocks: [], count: 0, invalid: 0 })
  assert.deepEqual(parseVoxelData('foo'), { blocks: [], count: 0, invalid: 0 })
  assert.deepEqual(parseVoxelData({}), { blocks: [], count: 0, invalid: 0 })
  assert.deepEqual(parseVoxelData(42), { blocks: [], count: 0, invalid: 0 })
})

test('长度非 4 的四元组被跳过并计数', () => {
  const raw = [
    ['minecraft:stone', 0, 0, 0],
    ['minecraft:stone', 0, 0],
    ['minecraft:stone', 0, 0, 0, 9],
  ]
  const result = parseVoxelData(raw)
  assert.equal(result.count, 1)
  assert.equal(result.invalid, 2)
})

test('元素非数组时跳过并计数', () => {
  const raw = [['minecraft:stone', 0, 0, 0], 'foo', null, 123]
  const result = parseVoxelData(raw)
  assert.equal(result.count, 1)
  assert.equal(result.invalid, 3)
})

test('id 为空或非字符串被跳过', () => {
  const raw = [
    ['minecraft:stone', 0, 0, 0],
    ['', 0, 0, 0],
    [123, 0, 0, 0],
    [null, 0, 0, 0],
  ]
  const result = parseVoxelData(raw)
  assert.equal(result.count, 1)
  assert.equal(result.invalid, 3)
})

test('坐标为 NaN 或 Infinity 被跳过', () => {
  const raw = [
    ['minecraft:stone', 0, 0, 0],
    ['minecraft:stone', NaN, 0, 0],
    ['minecraft:stone', 0, Infinity, 0],
    ['minecraft:stone', 0, 0, -Infinity],
    ['minecraft:stone', 0, 0, '5'],
  ]
  const result = parseVoxelData(raw)
  assert.equal(result.count, 1)
  assert.equal(result.invalid, 4)
})

test('合法四元组被正确规范化', () => {
  const raw = [
    ['minecraft:stone', 0, 1, 2],
    ['minecraft:glass', 3, 4, 5],
    ['minecraft:dirt', -1, -2, -3],
  ]
  const result = parseVoxelData(raw)
  assert.equal(result.count, 3)
  assert.equal(result.invalid, 0)
  assert.deepEqual(result.blocks, [
    { id: 'minecraft:stone', x: 0, y: 1, z: 2 },
    { id: 'minecraft:glass', x: 3, y: 4, z: 5 },
    { id: 'minecraft:dirt', x: -1, y: -2, z: -3 },
  ])
})