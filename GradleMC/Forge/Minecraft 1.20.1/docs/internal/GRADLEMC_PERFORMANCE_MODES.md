# GradleMC performance modes — current audit

LOW_IMPACT, BALANCED (default), and DETAILED currently expose maximum heavy
tasks and overlay/GUI refresh intervals. They are not yet a single immutable
policy applied to all required consumers. DETAILED must not be presented as
complete until the required no-idle-work and temporary-override tests exist.
