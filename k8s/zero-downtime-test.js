/**
 * Zero-downtime test script.
 *
 * Continuously hits an endpoint during a rollout and reports
 * any failed requests. Run this BEFORE triggering the rollout,
 * then trigger the rollout in another terminal.
 *
 * Usage:
 *   node zero-downtime-test.js [url] [duration_seconds] [interval_ms]
 *
 * Defaults:
 *   url              = http://192.168.49.2:30090/api/users/
 *   duration_seconds = 120
 *   interval_ms      = 200
 */

const url = process.argv[2] || "http://192.168.49.2:30090/api/users/";
const durationSec = parseInt(process.argv[3] || "600", 10);
const intervalMs = parseInt(process.argv[4] || "200", 10);

// You'll need a valid JWT — paste one here or pass via AUTH_TOKEN env var
const token = process.env.AUTH_TOKEN || "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsImNsaWVudElkIjoiQ0xJRU5ULTAwMSIsInJvbGVzIjpbIkFETUlOIiwiTUFOQUdFUiJdLCJ3b3JrRW50aXRpZXMiOlsiRU5USVRZLUEiLCJFTlRJVFktQiIsIkVOVElUWS1DIl0sImlhdCI6MTc3NzA4MzgzNywiZXhwIjoxNzc3MDg0MTM3fQ.3lcUrQzaSJT4pLSE8HN8VxI7Cx0VdwL-8c0VvShgc-ya3mkSJSg4DRd1bpP5-iS-";

let total = 0;
let success = 0;
let failed = 0;
const failures = [];

async function ping() {
  const start = Date.now();
  total++;

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);

    const headers = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const res = await fetch(url, { signal: controller.signal, headers });
    clearTimeout(timeout);

    const latency = Date.now() - start;

    if (res.ok || res.status === 401 || res.status === 403) {
      // 401/403 still means the server responded — not downtime
      success++;
      process.stdout.write(`\r#${total}  ${res.status}  ${latency}ms  ok:${success} fail:${failed}\n`);
    } else {
      failed++;
      const entry = { request: total, status: res.status, latency, time: new Date().toISOString() };
      failures.push(entry);
      process.stdout.write(`\r#${total}  ${res.status}  ${latency}ms  ok:${success} fail:${failed}\n  *** FAIL ***`);
    }
  } catch (err) {
    failed++;
    const latency = Date.now() - start;
    const entry = { request: total, error: err.message, latency, time: new Date().toISOString() };
    failures.push(entry);
    process.stdout.write(`\r#${total}  ERR  ${latency}ms  ok:${success} fail:${failed}\n  *** FAIL ***`);
  }
}

function report() {
  console.log("\n");
  console.log("========== ZERO-DOWNTIME TEST RESULTS ==========");
  console.log(`URL:        ${url}`);
  console.log(`Duration:   ${durationSec}s (every ${intervalMs}ms)`);
  console.log(`Total:      ${total}`);
  console.log(`Success:    ${success}`);
  console.log(`Failed:     ${failed}`);
  console.log(`Uptime:     ${((success / total) * 100).toFixed(2)}%`);
  console.log("");

  if (failures.length === 0) {
    console.log("RESULT: ZERO DOWNTIME — all requests served.");
  } else {
    console.log(`RESULT: ${failures.length} FAILURES DETECTED`);
    console.log("");
    console.log("Failed requests:");
    failures.forEach((f) => {
      const reason = f.status ? `HTTP ${f.status}` : f.error;
      console.log(`  #${f.request} at ${f.time} — ${reason} (${f.latency}ms)`);
    });
  }

  console.log("================================================");
}

console.log(`Testing ${url}`);
console.log(`Duration: ${durationSec}s | Interval: ${intervalMs}ms`);
console.log(`Trigger your rollout now in another terminal...\n`);

const timer = setInterval(ping, intervalMs);

setTimeout(() => {
  clearInterval(timer);
  setTimeout(report, 500);
}, durationSec * 1000);
