/**
 * Blue-green consistency test.
 *
 * Tracks the first user's name (id=1) across requests.
 * During a blue-green rollout, the name should switch exactly once
 * from old value to new value — never flip-flop back and forth.
 *
 * Steps:
 *   1. Run this script
 *   2. Change "John Doex12" to something else in UserController.java
 *   3. Run ./k8s/rollout.sh user-service in another terminal
 *   4. Watch for a single clean switch below
 *
 * Usage:
 *   node bluegreen-test.js [url] [duration_seconds] [interval_ms]
 */

const url = process.argv[2] || "http://192.168.49.2:30090/api/users/";
const durationSec = parseInt(process.argv[3] || "240", 10);
const intervalMs = parseInt(process.argv[4] || "200", 10);

const token = process.env.AUTH_TOKEN || "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsImNsaWVudElkIjoiQ0xJRU5ULTAwMSIsInJvbGVzIjpbIkFETUlOIiwiTUFOQUdFUiJdLCJ3b3JrRW50aXRpZXMiOlsiRU5USVRZLUEiLCJFTlRJVFktQiIsIkVOVElUWS1DIl0sImlhdCI6MTc3NzA5MTcyNywiZXhwIjoxNzc3MDkyMDI3fQ.nHvOY--A6M6_AeGoQRqEiLRbo6l9ISf86AUojEM3j3MKljU2r6wuifTzNH4dtUGG";

let total = 0;
let success = 0;
let failed = 0;

let currentName = null;
const switches = []; // each time the name changes

async function poll() {
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

    if (!res.ok) {
      if (res.status === 401 || res.status === 403) {
        success++;
      } else {
        failed++;
      }
      process.stdout.write(
        `\r#${total}  ${res.status}  ${latency}ms  switches:${switches.length}  failed:${failed}`
      );
      return;
    }

    const body = await res.json();
    const users = body.users || [];
    const firstUser = users.find((u) => u.id === 1);
    const name = firstUser?.name || "unknown";

    // Detect switch
    if (currentName !== null && currentName !== name) {
      switches.push({
        request: total,
        time: new Date().toISOString(),
        from: currentName,
        to: name,
        latency,
      });
    }
    currentName = name;

    success++;
    process.stdout.write(
      `\r#${total}  200  ${latency}ms  switches:${switches.length}  name: "${name}"                    `
    );
  } catch (err) {
    failed++;
    process.stdout.write(
      `\r#${total}  ERR  ${err.message.substring(0, 30)}  switches:${switches.length}  failed:${failed}`
    );
  }
}

function report() {
  console.log("\n");
  console.log("========== BLUE-GREEN CONSISTENCY TEST ==========");
  console.log(`URL:          ${url}`);
  console.log(`Duration:     ${durationSec}s (every ${intervalMs}ms)`);
  console.log(`Total:        ${total}`);
  console.log(`Success:      ${success}`);
  console.log(`Failed:       ${failed}`);
  console.log(`Switches:     ${switches.length}`);
  console.log("");

  if (switches.length === 0) {
    console.log("RESULT: No name change detected. Did you deploy a new version?");
  } else if (switches.length === 1) {
    const sw = switches[0];
    console.log(`RESULT: CLEAN CUTOVER — name switched exactly once.`);
    console.log(`  At request #${sw.request} (${sw.time})`);
    console.log(`  "${sw.from}" -> "${sw.to}"`);
  } else {
    console.log(`RESULT: FLIP-FLOP DETECTED — name switched ${switches.length} times!`);
    console.log("        Traffic is hitting both old and new versions.");
    console.log("");
    for (const sw of switches) {
      console.log(`  #${sw.request} at ${sw.time}: "${sw.from}" -> "${sw.to}"`);
    }
  }
  console.log("=================================================");
}

console.log(`Blue-green test: tracking first user's name`);
console.log(`URL: ${url}`);
console.log(`Duration: ${durationSec}s | Interval: ${intervalMs}ms`);
console.log("");
console.log("Steps:");
console.log('  1. This script is now polling...');
console.log('  2. Change "John Doex12" in UserController.java');
console.log("  3. Run: ./k8s/rollout.sh user-service");
console.log("");

const timer = setInterval(poll, intervalMs);

setTimeout(() => {
  clearInterval(timer);
  setTimeout(report, 500);
}, durationSec * 1000);
