import { mkdir, readFile, writeFile } from "node:fs/promises";
import { existsSync, statSync } from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const reportPath = path.join(repoRoot, "build", "reports", "gradlemc", "node-tooling-report.txt");

type CheckMode = "docs" | "curseforge";

interface CheckResult {
  area: string;
  ok: boolean;
  detail: string;
}

const args = new Set(process.argv.slice(2));
const modes: CheckMode[] = args.has("--docs")
  ? ["docs"]
  : args.has("--curseforge")
    ? ["curseforge"]
    : ["docs", "curseforge"];

const markdownFiles = [
  "README.md",
  "AGENTS.md",
  "docs/AUTOMATION_PIPELINE.md",
  "docs/PORTING_MATRIX.md",
  "docs/COMMON_CORE_EXTRACTION.md",
  "docs/PROFILING_ROADMAP.md"
];

const curseForgeCandidates = [
  "curseforge-description.html",
  "curseforge-description"
];

const voidTags = new Set([
  "area",
  "base",
  "br",
  "col",
  "embed",
  "hr",
  "img",
  "input",
  "link",
  "meta",
  "param",
  "source",
  "track",
  "wbr"
]);

async function readText(relativePath: string): Promise<string> {
  return readFile(path.join(repoRoot, relativePath), "utf8");
}

function isLocalMarkdownTarget(target: string): boolean {
  const trimmed = target.trim();
  return Boolean(trimmed)
    && !trimmed.startsWith("#")
    && !/^[a-z][a-z0-9+.-]*:/i.test(trimmed)
    && !trimmed.startsWith("mailto:")
    && !trimmed.startsWith("data:");
}

function stripAnchor(target: string): string {
  const hash = target.indexOf("#");
  return hash >= 0 ? target.slice(0, hash) : target;
}

function markdownLinks(text: string): string[] {
  const links: string[] = [];
  const pattern = /!?\[[^\]]*]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;
  for (const match of text.matchAll(pattern)) {
    links.push(match[1]);
  }
  return links;
}

function findMissingLocalLinks(file: string, text: string): string[] {
  const baseDir = path.dirname(path.join(repoRoot, file));
  const missing: string[] = [];
  for (const rawTarget of markdownLinks(text)) {
    const target = decodeURIComponent(stripAnchor(rawTarget));
    if (!isLocalMarkdownTarget(target) || target === "") {
      continue;
    }
    const resolved = path.resolve(baseDir, target);
    if (!resolved.startsWith(repoRoot) || !existsSync(resolved)) {
      missing.push(`${file} -> ${rawTarget}`);
    }
  }
  return missing;
}

function findWorkflowClaimErrors(file: string, text: string): string[] {
  const errors: string[] = [];
  const claimed = ".github/workflows/gradlemc-matrix.yml";
  if (text.includes(claimed) && !existsSync(path.join(repoRoot, claimed))) {
    errors.push(`${file} claims missing workflow ${claimed}`);
  }
  return errors;
}

async function checkDocs(): Promise<CheckResult[]> {
  const results: CheckResult[] = [];
  const missingLinks: string[] = [];
  const workflowErrors: string[] = [];

  for (const file of markdownFiles) {
    const fullPath = path.join(repoRoot, file);
    if (!existsSync(fullPath)) {
      continue;
    }
    const text = await readText(file);
    missingLinks.push(...findMissingLocalLinks(file, text));
    workflowErrors.push(...findWorkflowClaimErrors(file, text));
  }

  results.push({
    area: "docs:local-links",
    ok: missingLinks.length === 0,
    detail: missingLinks.length === 0 ? "all checked local Markdown links resolve" : missingLinks.join("; ")
  });
  results.push({
    area: "docs:workflow-claims",
    ok: workflowErrors.length === 0,
    detail: workflowErrors.length === 0 ? "workflow path claims match checked-in files" : workflowErrors.join("; ")
  });
  return results;
}

function findBrokenObviousTags(html: string): string[] {
  const errors: string[] = [];
  const stack: string[] = [];
  const tagPattern = /<\/?([a-zA-Z][a-zA-Z0-9:-]*)(?:\s[^>]*)?>/g;
  for (const match of html.matchAll(tagPattern)) {
    const raw = match[0];
    const tag = match[1].toLowerCase();
    if (raw.startsWith("<!") || voidTags.has(tag) || raw.endsWith("/>")) {
      continue;
    }
    if (raw.startsWith("</")) {
      const previous = stack.pop();
      if (previous !== tag) {
        errors.push(`unexpected closing </${tag}>${previous ? ` after <${previous}>` : ""}`);
      }
    } else {
      stack.push(tag);
    }
  }
  for (const tag of stack.reverse()) {
    errors.push(`missing closing </${tag}>`);
  }
  return errors;
}

async function checkCurseForge(): Promise<CheckResult[]> {
  const existing = curseForgeCandidates.find((candidate) => existsSync(path.join(repoRoot, candidate)));
  if (!existing) {
    return [{
      area: "curseforge:presence",
      ok: true,
      detail: "no CurseForge description file is present; validation skipped without recreating it"
    }];
  }

  const fullPath = path.join(repoRoot, existing);
  const text = await readText(existing);
  const lower = text.toLowerCase();
  const errors: string[] = [];

  if (statSync(fullPath).size > 250_000) {
    errors.push(`${existing} is unexpectedly large for a store description`);
  }
  if (/<script\b/i.test(text)) {
    errors.push("script tags are not allowed");
  }
  if (/\b(googletagmanager|google-analytics|gtag\(|analytics\.js|facebook\.net|pixel)\b/i.test(text)) {
    errors.push("external tracking references are not allowed");
  }
  if (/\/GradleMC\b/.test(text)) {
    errors.push("uppercase /GradleMC command examples are not allowed");
  }
  if (/GradleMC Forge 1\.20\.1/.test(text)) {
    errors.push("do not use loader/version as the project name");
  }
  if (/(fabric|neoforge)[^.\n]{0,80}\b(supported|available|download|ready|works)\b/i.test(text)) {
    errors.push("Fabric/NeoForge must not be described as supported or ready");
  }
  if (/GradleMC Forge 1\.20\.1|GradleMC%20Forge%201\.20\.1/i.test(text)) {
    errors.push("stale old folder path or project-name wording detected");
  }
  if (/(box-shadow\s*:\s*0\s+0\s+0\s+\d{2,}px|outline\s*:\s*\d{2,}px|border\s*:\s*\d{2,}px)/i.test(text)) {
    errors.push("giant box-outline CSS is not allowed in the clean description");
  }
  if (lower.includes("<html") || lower.includes("<body")) {
    errors.push("use a fragment-style description, not a full HTML document");
  }

  errors.push(...findBrokenObviousTags(text));

  return [{
    area: "curseforge:html",
    ok: errors.length === 0,
    detail: errors.length === 0 ? `${existing} passed web-facing checks` : errors.join("; ")
  }];
}

async function main(): Promise<number> {
  const results: CheckResult[] = [];
  if (modes.includes("docs")) {
    results.push(...await checkDocs());
  }
  if (modes.includes("curseforge")) {
    results.push(...await checkCurseForge());
  }

  await mkdir(path.dirname(reportPath), { recursive: true });
  const report = [
    "GradleMC Node tooling report",
    "============================",
    "",
    `Modes: ${modes.join(", ")}`,
    "",
    ...results.map((result) => `${result.ok ? "OK" : "ERROR"}: ${result.area}: ${result.detail}`),
    ""
  ].join("\n");
  await writeFile(reportPath, report, "utf8");
  process.stdout.write(report);

  return results.every((result) => result.ok) ? 0 : 1;
}

main().then((exitCode) => {
  process.exitCode = exitCode;
}).catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
