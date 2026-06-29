#!/usr/bin/env node
import { existsSync } from "node:fs";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

const compiled = resolve("build/node/check-docs.js");

if (!existsSync(compiled)) {
  console.error("Missing compiled TypeScript output. Run npm run build first.");
  process.exit(1);
}

await import(pathToFileURL(compiled).href);
