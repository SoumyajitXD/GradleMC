# GradleMC platform architecture

GradleMC is a local Minecraft diagnostic orchestration project. Lifecycle: Discovery, Planning, Execution, Finalisation. Discovery is cheap and bounded; planning resolves a validated deterministic graph; execution uses declared owning threads and budgets; finalisation records results even after failure or cancellation. It is not a compiler, scripting engine, remote service, plugin downloader, or automatic repair system.

Roadmap separates v1.0.4 recovery, kernel foundations, later public-API stabilisation, multi-loader work, opt-in bounded JFR, and modpack validation. Later items are not implemented by this document.
