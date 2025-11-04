package com.veonoes.ar

class PoseSmoother(
  private val alphaPos: Float = 0.25f,  // 0..1 (düşük = daha çok yumuşatma)
  private val alphaRot: Float = 0.25f,
  private val alphaScale: Float = 0.25f
) {
  private var last: Pose? = null

  fun filter(p: Pose): Pose {
    val prev = last ?: run { last = p; return p }
    val nx = prev.cx + alphaPos * (p.cx - prev.cx)
    val ny = prev.cy + alphaPos * (p.cy - prev.cy)

    // açı sarmalaması gerekmiyorsa basit yaklaşım yeter
    val nr = prev.rotationDeg + alphaRot * (p.rotationDeg - prev.rotationDeg)
    val ns = prev.scale + alphaScale * (p.scale - prev.scale)

    return Pose(nx, ny, nr, ns).also { last = it }
  }
}
