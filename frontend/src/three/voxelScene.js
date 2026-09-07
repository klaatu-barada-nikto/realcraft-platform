import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { blockColorMap, DEFAULT_MISSING_COLOR } from './blockColorMap'

const FOV = 75
const CAMERA_SAFETY_FACTOR = 1.5

export class VoxelScene {
  constructor(container) {
    this.container = container
    this.renderer = null
    this.scene = null
    this.camera = null
    this.controls = null
    this.mesh = null
    this.rafId = null
    this.resizeObserver = null
    this.boundingBox = null
  }

  mount() {
    const width = this.container.clientWidth || 1
    const height = this.container.clientHeight || 1

    try {
      this.renderer = new THREE.WebGLRenderer({ antialias: true })
    } catch (e) {
      return { supported: false }
    }

    if (!this.renderer.getContext()) {
      this.renderer.dispose()
      this.renderer = null
      return { supported: false }
    }

    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
    this.renderer.setSize(width, height)

    this.scene = new THREE.Scene()
    this.scene.background = new THREE.Color(0x0d120d)

    this.camera = new THREE.PerspectiveCamera(FOV, width / height, 0.1, 100000)
    this.camera.position.set(0, 0, 10)

    const ambient = new THREE.AmbientLight(0xffffff, 0.6)
    const directional = new THREE.DirectionalLight(0xffffff, 0.8)
    directional.position.set(10, 20, 10)
    this.scene.add(ambient)
    this.scene.add(directional)

    this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.08
    this.controls.target.set(0, 0, 0)

    this.container.appendChild(this.renderer.domElement)

    this.resizeObserver = new ResizeObserver(() => this._handleResize())
    this.resizeObserver.observe(this.container)

    const animate = () => {
      this.rafId = requestAnimationFrame(animate)
      if (this.container.clientWidth === 0 || this.container.clientHeight === 0) {
        return
      }
      this.controls.update()
      this.renderer.render(this.scene, this.camera)
    }
    animate()

    return { supported: true }
  }

  setVoxels(blocks) {
    this.clear()

    if (!blocks || blocks.length === 0) {
      return
    }

    const count = blocks.length
    const geometry = new THREE.BoxGeometry(1, 1, 1)
    const material = new THREE.MeshLambertMaterial({ color: 0xffffff, transparent: false })

    const mesh = new THREE.InstancedMesh(geometry, material, count)

    const dummy = new THREE.Object3D()
    const tempColor = new THREE.Color()
    const missingIds = new Set()

    let minX = Infinity
    let minY = Infinity
    let minZ = Infinity
    let maxX = -Infinity
    let maxY = -Infinity
    let maxZ = -Infinity

    for (let i = 0; i < count; i += 1) {
      const block = blocks[i]
      const { x, y, z } = block

      dummy.position.set(x, y, z)
      dummy.scale.set(1, 1, 1)
      dummy.updateMatrix()
      mesh.setMatrixAt(i, dummy.matrix)

      const hex = blockColorMap[block.id] || DEFAULT_MISSING_COLOR
      if (!blockColorMap[block.id]) {
        missingIds.add(block.id)
      }
      tempColor.set(hex)
      mesh.setColorAt(i, tempColor)

      if (x < minX) minX = x
      if (y < minY) minY = y
      if (z < minZ) minZ = z
      if (x + 1 > maxX) maxX = x + 1
      if (y + 1 > maxY) maxY = y + 1
      if (z + 1 > maxZ) maxZ = z + 1
    }

    mesh.instanceMatrix.needsUpdate = true
    if (mesh.instanceColor) {
      mesh.instanceColor.needsUpdate = true
    }

    for (const id of missingIds) {
      console.warn(`[VoxelScene] 未注册方块标识符，使用缺省颜色: ${id}`)
    }

    const center = new THREE.Vector3(
      (minX + maxX) / 2,
      (minY + maxY) / 2,
      (minZ + maxZ) / 2,
    )
    const size = new THREE.Vector3(maxX - minX, maxY - minY, maxZ - minZ)

    mesh.position.set(-center.x, -center.y, -center.z)
    this.scene.add(mesh)
    this.mesh = mesh
    this.boundingBox = new THREE.Box3(
      new THREE.Vector3(-size.x / 2, -size.y / 2, -size.z / 2),
      new THREE.Vector3(size.x / 2, size.y / 2, size.z / 2),
    )

    this._fitCamera(size)
  }

  _fitCamera(size) {
    const maxDim = Math.max(size.x, size.y, size.z)
    if (!Number.isFinite(maxDim) || maxDim <= 0) {
      this.camera.position.set(0, 0, 10)
      this.controls.target.set(0, 0, 0)
      this.controls.update()
      return
    }

    const distance = (maxDim / 2) / Math.tan((FOV * Math.PI) / 360) * CAMERA_SAFETY_FACTOR

    this.camera.position.set(distance * 0.6, distance * 0.5, distance)
    this.camera.near = Math.max(0.1, distance / 1000)
    this.camera.far = distance * 100
    this.camera.updateProjectionMatrix()

    this.controls.target.set(0, 0, 0)
    this.controls.update()
  }

  _handleResize() {
    const width = this.container.clientWidth
    const height = this.container.clientHeight
    if (width === 0 || height === 0 || !this.renderer || !this.camera) {
      return
    }
    this.renderer.setSize(width, height)
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
  }

  clear() {
    if (this.mesh) {
      this.scene.remove(this.mesh)
      this.mesh.geometry.dispose()
      this.mesh.material.dispose()
      this.mesh = null
    }
    this.boundingBox = null
  }

  dispose() {
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId)
      this.rafId = null
    }

    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }

    this.clear()

    if (this.controls) {
      this.controls.dispose()
      this.controls = null
    }

    if (this.renderer) {
      this.renderer.dispose()
      if (this.renderer.domElement && this.renderer.domElement.parentNode === this.container) {
        this.container.removeChild(this.renderer.domElement)
      }
      this.renderer = null
    }

    this.scene = null
    this.camera = null
  }
}