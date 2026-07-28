# Targeted execution-surface security model

Only registered bounded lowercase IDs are accepted; no class names, paths, shell text or scripts are accepted. Required dependencies, permissions, sides, capabilities, budgets and timeouts are checked before execution. Cycle/depth checks limit graph denial of service. Output is local and managed; runtime evidence is never cached as current evidence. Persistent cache/history schema hardening remains future work.
