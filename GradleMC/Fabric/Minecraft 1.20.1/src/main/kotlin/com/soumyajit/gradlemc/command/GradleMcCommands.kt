package com.soumyajit.gradlemc.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.soumyajit.gradlemc.GradleMC
import com.soumyajit.gradlemc.config.GradleMcConfig
import com.soumyajit.gradlemc.diagnostics.*
import com.soumyajit.gradlemc.network.GradleMcNetwork
import com.soumyajit.gradlemc.performance.*
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import java.util.Locale

/** Common-only, testable command surface. Client UI classes never appear here. */
object GradleMcCommands {
    fun register(d: CommandDispatcher<CommandSourceStack>) {
        d.register(Commands.literal("gradlemc").executes { help(it.source) }
            .then(literal("help") { help(it) }).then(literal("version") { version(it) }).then(literal("status") { status(it) })
            .then(literal("memory") { ok(it, DiagnosticsService.memoryLine()) }).then(literal("gui") { gui(it) })
            .then(testFps()).then(perf()).then(performance()).then(overlay())
            .then(Commands.literal("check").requires { it.hasPermission(2) }.executes { check(it.source) })
            .then(Commands.literal("export").requires { it.hasPermission(2) }.executes { export(it.source) })
            .then(reports()).then(issueBundle()).then(files()).then(config()).then(mods()).then(entities(false)).then(entities(true)).then(smart()))
    }
    private fun literal(word:String, run:(CommandSourceStack)->Int)=Commands.literal(word).executes { run(it.source) }
    private fun help(s:CommandSourceStack)=ok(s,listOf("GradleMC commands:","General: gui, version, status, memory, help.","Diagnostics: check, files, config path|files|check, entities [radius], blockentities [radius].","Reports: export, reports list|latest, issuebundle create.","Mods: mods count|list|search <id>|inspect <id>|audit|export.","Smart: smart score|advice|explain|thresholds.","FPS/performance: testfps start <seconds>|stop; perf start <seconds>|<seconds>|stop; performance [overhead|guard|explain|selftest|mode ...].","Overlay: overlay status|on|off|toggle|title on|off|fps on|off|average on|off|window 30|60|120|reset."))
    private fun version(s:CommandSourceStack)=ok(s,listOf("GradleMC version: ${GradleMC.VERSION}","Minecraft: ${GradleMC.MINECRAFT_VERSION}","Loader: Fabric","Java runtime: ${System.getProperty("java.version")}","Kotlin runtime: ${KotlinVersion.CURRENT}"))
    private fun status(s:CommandSourceStack)=DiagnosticsService.snapshot().let { x -> ok(s,listOf("GradleMC status: ${x.environment.physicalSide.lowercase()}, ${x.environment.installedModCount} mods, performance mode ${x.performance.mode.label}.",DiagnosticsService.memoryLine(),"FPS: ${fps(x.performance.currentFps)}; average: ${fps(x.performance.averageFps)}.","Overlay: ${if(x.configuration.overlayEnabled) "enabled" else "disabled"}; latest report: ${x.latestReport?.let(DiagnosticsService::displayPath) ?: "none"}.")) }
    private fun testFps()=Commands.literal("testfps").executes { ok(it.source,"Use /gradlemc testfps start <seconds> or stop.") }.then(Commands.literal("start").then(Commands.argument("seconds",IntegerArgumentType.integer(5,1800)).executes { fpsAction(it.source,IntegerArgumentType.getInteger(it,"seconds")) })).then(literal("stop") { fpsAction(it,null) })
    /** FPS intervals belong to the physical client render loop, not to a server command thread. */
    private fun fpsAction(s:CommandSourceStack, seconds:Int?):Int {
        val player=s.entity as? ServerPlayer?:return fail(s,"FPS testing requires an in-game player with GradleMC installed on the client.")
        if (!GradleMcNetwork.requestFpsTest(player,seconds)) return fail(s,"A compatible GradleMC Fabric client is required for FPS testing.")
        return ok(s,if(seconds==null) "Requested that the client stop its GradleMC FPS test." else "Requested a ${seconds}s GradleMC FPS test on the client.")
    }
    private fun perf()=Commands.literal("perf").requires { it.hasPermission(2) }.executes { ok(it.source,"Use /gradlemc perf start <seconds>, /gradlemc perf <seconds>, or stop.") }.then(Commands.literal("start").then(Commands.argument("seconds",IntegerArgumentType.integer(5,1800)).executes { action(it.source,PerformanceService.startTimedSample(IntegerArgumentType.getInteger(it,"seconds"))) })).then(literal("stop") { action(it,PerformanceService.stopTimedSample()) }).then(Commands.argument("seconds",IntegerArgumentType.integer(5,1800)).executes { action(it.source,PerformanceService.startTimedSample(IntegerArgumentType.getInteger(it,"seconds"))) })
    private fun performance()=Commands.literal("performance").executes { performanceSummary(it.source) }.then(literal("overhead") { ok(it,PerformanceService.overheadDescription()) }).then(literal("guard") { ok(it,PerformanceService.guardDescription()) }).then(literal("explain") { ok(it,PerformanceService.explainDescription()) }).then(Commands.literal("selftest").requires { it.hasPermission(2) }.executes { ok(it.source,"Performance self-test: ${if(PerformanceService.selfTest()) "passed" else "failed"}.") }).then(Commands.literal("mode").executes { performanceSummary(it.source) }.then(mode("low_impact",PerformanceMode.LOW_IMPACT)).then(mode("balanced",PerformanceMode.BALANCED)).then(mode("detailed",PerformanceMode.DETAILED)))
    private fun mode(word:String,value:PerformanceMode)=Commands.literal(word).requires { it.hasPermission(2) }.executes { actionMode(it.source,PerformanceService.setMode(value)) }
    private fun performanceSummary(s:CommandSourceStack)=PerformanceService.snapshot().let { ok(s,"Performance: ${it.mode.label}; current ${fps(it.currentFps)} FPS; average ${fps(it.averageFps)} FPS. ${it.message}") }
    private fun overlay()=Commands.literal("overlay").executes { overlayStatus(it.source) }.then(literal("status") { overlayStatus(it) }).then(overlaySet("on") { it.copy(overlayEnabled=true) }).then(overlaySet("off") { it.copy(overlayEnabled=false) }).then(overlaySet("toggle") { it.copy(overlayEnabled=!it.overlayEnabled) }).then(component("title") { c,v->c.copy(overlayShowTitle=v) }).then(component("fps") { c,v->c.copy(overlayShowFps=v) }).then(component("average") { c,v->c.copy(overlayShowAverageFps=v) }).then(Commands.literal("window").then(Commands.argument("seconds",IntegerArgumentType.integer()).executes { overlayUpdate(it.source) { c->c.copy(overlaySamplingWindowSeconds=IntegerArgumentType.getInteger(it,"seconds")) } })).then(overlaySet("reset") { com.soumyajit.gradlemc.config.GradleMcConfigSnapshot() })
    private fun overlaySet(word:String, f:(com.soumyajit.gradlemc.config.GradleMcConfigSnapshot)->com.soumyajit.gradlemc.config.GradleMcConfigSnapshot)=literal(word) { overlayUpdate(it,f) }
    private fun component(word:String, f:(com.soumyajit.gradlemc.config.GradleMcConfigSnapshot,Boolean)->com.soumyajit.gradlemc.config.GradleMcConfigSnapshot)=Commands.literal(word).then(literal("on") { overlayUpdate(it) { c->f(c,true) } }).then(literal("off") { overlayUpdate(it) { c->f(c,false) } })
    private fun overlayUpdate(s:CommandSourceStack,f:(com.soumyajit.gradlemc.config.GradleMcConfigSnapshot)->com.soumyajit.gradlemc.config.GradleMcConfigSnapshot):Int { if(FabricLoader.getInstance().environmentType!=EnvType.CLIENT) return fail(s,"Overlay settings are client-owned. On multiplayer use /gradlemc-overlay on your client."); return try { GradleMcConfig.update(f); PerformanceService.configureFromConfig(); overlayStatus(s) } catch(e:Exception) { fail(s,"Unable to persist overlay settings: ${e.message ?: e.javaClass.simpleName}") } }
    private fun overlayStatus(s:CommandSourceStack)=GradleMcConfig.current().let { ok(s,"Overlay ${if(it.overlayEnabled)"on" else "off"}; title=${it.overlayShowTitle}, fps=${it.overlayShowFps}, average=${it.overlayShowAverageFps}, window=${it.overlaySamplingWindowSeconds}s.") }
    private fun reports()=Commands.literal("reports").executes { ok(it.source,"Use /gradlemc reports list or latest.") }.then(literal("latest") { latest(it) }).then(literal("list") { val r=DiagnosticSupport.reports(); if(r.isEmpty()) ok(it,"No GradleMC reports found.") else ok(it,listOf("GradleMC reports (newest first):")+r.map(DiagnosticsService::displayPath)) })
    private fun issueBundle()=Commands.literal("issuebundle").requires { it.hasPermission(2) }.executes { ok(it.source,"Use /gradlemc issuebundle create.") }.then(literal("create") { when(val r=DiagnosticSupport.createIssueBundle()){is IssueBundleResult.Success->ok(it,"GradleMC issue bundle created: ${r.displayPath}");is IssueBundleResult.Failure->fail(it,r.message)} })
    private fun files()=literal("files") { val l=DiagnosticSupport.locations();ok(it,listOf("GradleMC output: ${DiagnosticsService.displayPath(l.outputRoot)}","Reports: ${DiagnosticsService.displayPath(l.reports)}","Config: ${DiagnosticsService.displayPath(l.configuration)}","Latest report: ${l.latestReport?.let(DiagnosticsService::displayPath)?:"none"}")) }
    private fun config()=Commands.literal("config").executes { ok(it.source,"Use /gradlemc config path, files, or check.") }.then(literal("path") { ok(it,DiagnosticsService.displayPath(DiagnosticSupport.locations().configuration)) }).then(literal("files") { val l=DiagnosticSupport.locations();ok(it,listOf("Config: ${DiagnosticsService.displayPath(l.configuration)}","Output root: ${DiagnosticsService.displayPath(l.outputRoot)}")) }).then(literal("check") { val r=DiagnosticSupport.checkConfig();if(r.valid)ok(it,r.messages)else fail(it,r.messages.joinToString(" ")) })
    private fun mods() = Commands.literal("mods").executes { ok(it.source, "Installed mods: ${ModTools.all().size}. Use count, list, search, inspect, audit, or export.") }
        .then(literal("count") { ok(it, "Installed mods: ${ModTools.all().size}.") })
        .then(literal("list") { ok(it, ModTools.all().take(40).map { m -> "${m.id} - ${m.name} ${m.version}" } + if (ModTools.all().size > 40) listOf("Output limited to 40; use /gradlemc mods export.") else emptyList()) })
        .then(Commands.literal("search").then(Commands.argument("modid", StringArgumentType.word()).executes { modFind(it.source, StringArgumentType.getString(it, "modid")) }))
        .then(Commands.literal("inspect").then(Commands.argument("modid", StringArgumentType.word()).executes { modFind(it.source, StringArgumentType.getString(it, "modid")) }))
        .then(literal("audit") { ok(it, ModTools.audit()) })
        .then(Commands.literal("export").requires { it.hasPermission(2) }.executes {
            val body = ModTools.all().joinToString("\n") { m -> "${m.id}\t${m.name}\t${m.version}\t${m.environment}" }
            try { val p = com.soumyajit.gradlemc.report.ReportFiles.write(DiagnosticSupport.locations().reports, "txt", body); ok(it.source, "Mod metadata export: ${DiagnosticsService.displayPath(p)}") }
            catch (e: Exception) { fail(it.source, "Mod metadata export failed: ${e.message ?: e.javaClass.simpleName}") }
        })
    private fun modFind(s:CommandSourceStack,id:String)=ModTools.find(id)?.let { ok(s,listOf("${it.id}: ${it.name}","Version: ${it.version}; environment: ${it.environment}")) } ?: fail(s,"No loaded mod matches '$id'.")
    private fun entities(block:Boolean)=Commands.literal(if(block)"blockentities" else "entities").requires { it.hasPermission(2) }.executes { scan(it.source,64,block) }.then(Commands.argument("radius",IntegerArgumentType.integer(8,128)).executes { scan(it.source,IntegerArgumentType.getInteger(it,"radius"),block) })
    private fun scan(s:CommandSourceStack,radius:Int,block:Boolean):Int { val p=s.entity as? ServerPlayer?:return fail(s,"This scan requires an in-game player.");val level=p.serverLevel(); if(!block){val found=level.getEntities(p,AABB.ofSize(p.position(),radius*2.0,radius*2.0,radius*2.0));return ok(s,"Entities within $radius blocks: ${found.size}.")} ;var total=0;var loaded=0; val pos=p.blockPosition();for(x in ((pos.x-radius) shr 4)..((pos.x+radius) shr 4))for(z in ((pos.z-radius) shr 4)..((pos.z+radius) shr 4)){val c=level.chunkSource.getChunk(x,z,ChunkStatus.FULL,false) as? LevelChunk?:continue;loaded++;total+=c.blockEntities.values.count { be->be.blockPos.distSqr(pos)<=radius.toDouble()*radius }};return ok(s,"Block entities within $radius blocks: $total across $loaded loaded chunks.") }
    private fun smart()=Commands.literal("smart").requires { it.hasPermission(2) }.executes { ok(it.source,"Use /gradlemc smart score, advice, explain, or thresholds.") }.then(literal("score") { smartScore(it) }).then(literal("advice") { smartAdvice(it) }).then(literal("explain") { ok(it,"Smart Diagnostics is local, deterministic and based on configuration, report output, Java compatibility and memory pressure checks.") }).then(literal("thresholds") { ok(it,"Memory warning threshold: 80%; critical: 95%. Scores subtract only deterministic warning/failure weights.") })
    private fun gui(s:CommandSourceStack):Int { val p=s.entity as? ServerPlayer?:return fail(s,"GradleMC GUI can only be opened by an in-game player.");return if(GradleMcNetwork.openGui(p))ok(s,"Requested GradleMC diagnostics on your client.")else fail(s,"A compatible GradleMC Fabric client is required to open the GUI.") }
    private fun check(s:CommandSourceStack)=DiagnosticsService.runChecks().let { c->ok(s,listOf("GradleMC checks: ${c.summary}; highest severity ${c.highestSeverity.name.lowercase()}.")+c.findings.filter { it.severity.ordinal>=DiagnosticSeverity.WARN.ordinal }.map { "[${it.severity}] ${it.title}: ${it.detail}" }) }
    private fun export(s:CommandSourceStack)=when(val r=DiagnosticsService.exportReport()){is ReportExportResult.Success->ok(s,"GradleMC diagnostics export written: ${r.displayPaths.joinToString(", ")}");is ReportExportResult.Failure->fail(s,r.message)}
    private fun latest(s:CommandSourceStack)=when(val r=DiagnosticsService.latestReportResult()){is LatestReportResult.Found->ok(s,"Latest GradleMC report: ${r.displayPath}");LatestReportResult.Empty->fail(s,"No GradleMC reports found yet. Run /gradlemc export to create one.");is LatestReportResult.Failure->fail(s,r.message)}
    private fun smartScore(s:CommandSourceStack)=DiagnosticsService.smartScore().let { ok(s,listOf("GradleMC Smart Diagnostics score: ${it.score}/100.",it.message,"Checks: ${it.basedOn.summary}.")) }
    private fun smartAdvice(s:CommandSourceStack)=DiagnosticsService.smartAdvice().let { ok(s,listOf("GradleMC Smart Diagnostics advice (score ${it.score}/100):")+it.advice.map { "- $it" }+it.message) }
    private fun action(s:CommandSourceStack,r:FpsTestActionResult)=if(r.success)ok(s,r.message)else fail(s,r.message);private fun actionMode(s:CommandSourceStack,r:PerformanceModeChangeResult)=if(r.success)ok(s,r.message)else fail(s,r.message)
    private fun fps(v:Double?)=v?.takeIf(Double::isFinite)?.let { "%.1f".format(Locale.ROOT,it) }?:"not collected"
    private fun ok(s:CommandSourceStack,m:String)=ok(s,listOf(m));private fun ok(s:CommandSourceStack,m:Iterable<String>):Int {m.forEach{s.sendSuccess({Component.literal(it)},false)};return 1};private fun fail(s:CommandSourceStack,m:String):Int{s.sendFailure(Component.literal(m));return 0}
}
