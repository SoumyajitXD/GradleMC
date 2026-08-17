package com.soumyajit.gradlemc.client.overlay

import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.performance.PerformanceService
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.roundToInt

internal object OverlayLineComposer { fun compose(title:Boolean, fps:Boolean, average:Boolean, current:Double?, avg:Double?):List<String> = buildList { if(title)add("GradleMC"); if(fps)add("FPS: ${format(current)}"); if(average)add("Average FPS: ${format(avg)}") }; private fun format(value:Double?)=value?.takeIf { it.isFinite() }?.coerceAtLeast(0.0)?.roundToInt()?.toString() ?: "warming up" }

/** HUD renderer does only cached text/layout work; data collection remains in PerformanceService. */
object GradleMcStatsOverlay {
    private var cached = emptyList<String>(); private var width = 0; private var refreshAt = 0L
    fun render(graphics: GuiGraphics, partialTick: Float) {
        val client=Minecraft.getInstance(); val c=GradleMcConfig.current(); val visible=c.overlayEnabled && client.level != null && client.player != null && client.screen == null && !client.options.renderDebug
        val demand=visible && (c.overlayShowFps || c.overlayShowAverageFps); PerformanceService.setOverlayFrameDemand(demand)
        if(!visible) { invalidate(); return }
        val now=System.nanoTime(); if(now>=refreshAt) { val s=PerformanceService.snapshot(); cached=OverlayLineComposer.compose(c.overlayShowTitle,c.overlayShowFps,c.overlayShowAverageFps,s.currentFps,s.averageFps); width=cached.maxOfOrNull { client.font.width(it) }?:0; refreshAt=now+500_000_000L }
        if(cached.isEmpty())return; val line=client.font.lineHeight+2; val h=cached.size*line+8; graphics.fill(6,6,6+width+8,6+h,0xA010141C.toInt());cached.forEachIndexed { i,text->graphics.drawString(client.font,text,10,10+i*line,0xFFEAF0F7.toInt()) }
    }
    fun invalidate(){cached=emptyList();refreshAt=0;PerformanceService.setOverlayFrameDemand(false)}
}
