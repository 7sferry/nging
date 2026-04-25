/**
 * Rate limiter test script.
 *
 * Sends a burst of concurrent requests to verify that nginx
 * returns 429 when the rate limit is exceeded.
 *
 * Nginx config:
 *   /api/auth/   — 5r/s  burst=10
 *   /api/users/  — 20r/s burst=40
 *
 * Usage:
 *   node rate-limit-test.js [url] [total_requests] [concurrency]
 *
 * Defaults:
 *   url              = http://192.168.49.2:30090/api/users/
 *   total_requests   = 100
 *   concurrency      = 50
 */

const url = process.argv[2] || "http://192.168.49.2:30090/api/users/";
const totalRequests = parseInt(process.argv[3] || "100", 10);
const concurrency = parseInt(process.argv[4] || "50", 10);

const token = process.env.AUTH_TOKEN || "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsImNsaWVudElkIjoiQ0xJRU5ULTAwMSIsInJvbGVzIjpbIkFETUlOIiwiTUFOQUdFUiJdLCJ3b3JrRW50aXRpZXMiOlsiRU5USVRZLUEiLCJFTlRJVFktQiIsIkVOVElUWS1DIl0sImlhdCI6MTc3NzA4MzQ5MywiZXhwIjoxNzc3MDgzNzkzfQ.I4XCJhHcyxgwA1_Aqd6kGc1m8DD5FvODrGnlntWBnGa4CBfOAq2BxkABYR9wGaK4";

const results = {};
let completed = 0;

async function send(id) {
  const start = Date.now();
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);

    const headers = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const res = await fetch(url, { signal: controller.signal, headers });
    clearTimeout(timeout);

    const latency = Date.now() - start;
    const status = res.status;
    results[status] = (results[status] || 0) + 1;

    completed++;
    process.stdout.write(`\r  sent: ${completed}/${totalRequests}`);
    return { id, status, latency };
  } catch (err) {
    const latency = Date.now() - start;
    const key = `ERR:${err.name}`;
    results[key] = (results[key] || 0) + 1;

    completed++;
    process.stdout.write(`\r  sent: ${completed}/${totalRequests}`);
    return { id, error: err.message, latency };
  }
}

async function runBatch(ids) {
  return Promise.all(ids.map((id) => send(id)));
}

async function main() {
  console.log("========== RATE LIMITER TEST ==========");
  console.log(`URL:          ${url}`);
  console.log(`Requests:     ${totalRequests}`);
  console.log(`Concurrency:  ${concurrency}`);
  console.log("");

  // --- Phase 1: Burst test (all at once) ---
  console.log("Phase 1: Burst — sending all requests concurrently...");
  const burstStart = Date.now();

  const batches = [];
  for (let i = 0; i < totalRequests; i += concurrency) {
    const batch = [];
    for (let j = i; j < Math.min(i + concurrency, totalRequests); j++) {
      batch.push(j + 1);
    }
    batches.push(batch);
  }

  const allResults = [];
  for (const batch of batches) {
    const batchResults = await runBatch(batch);
    allResults.push(...batchResults);
  }

  const burstDuration = Date.now() - burstStart;

  console.log("\n");
  console.log(`Completed in ${burstDuration}ms`);
  console.log("");

  // --- Report ---
  console.log("Response breakdown:");
  const sortedKeys = Object.keys(results).sort();
  for (const key of sortedKeys) {
    const count = results[key];
    const pct = ((count / totalRequests) * 100).toFixed(1);
    const label = key === "429" ? `${key} (rate limited)` : key;
    console.log(`  ${label}: ${count} (${pct}%)`);
  }

  console.log("");

  const rateLimited = results["429"] || 0;
  if (rateLimited > 0) {
    console.log(`RESULT: Rate limiter is WORKING — ${rateLimited}/${totalRequests} requests were throttled (429).`);
  } else {
    console.log("RESULT: No 429 responses — rate limiter may NOT be working, or burst/rate is too generous for this test.");
    console.log("        Try increasing total_requests or concurrency.");
  }

  // --- Phase 2: Sustained rate (just above limit) ---
  console.log("\n--- Phase 2: Sustained rate (above limit) ---");
  console.log("Sending 5 requests every 100ms for 3 seconds...\n");

  const phase2Results = {};
  let p2Total = 0;

  const p2Start = Date.now();
  const p2Duration = 3000;

  await new Promise((resolve) => {
    const interval = setInterval(async () => {
      if (Date.now() - p2Start >= p2Duration) {
        clearInterval(interval);
        resolve();
        return;
      }

      const promises = [];
      for (let i = 0; i < 5; i++) {
        p2Total++;
        promises.push(
          fetch(url, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
          })
            .then((res) => {
              phase2Results[res.status] = (phase2Results[res.status] || 0) + 1;
            })
            .catch((err) => {
              const key = `ERR:${err.name}`;
              phase2Results[key] = (phase2Results[key] || 0) + 1;
            })
        );
      }
      await Promise.all(promises);
    }, 100);
  });

  console.log(`Sent ${p2Total} requests over ${p2Duration}ms (50 req/s)`);
  console.log("Response breakdown:");
  for (const key of Object.keys(phase2Results).sort()) {
    const count = phase2Results[key];
    const pct = ((count / p2Total) * 100).toFixed(1);
    const label = key === "429" ? `${key} (rate limited)` : key;
    console.log(`  ${label}: ${count} (${pct}%)`);
  }

  const p2Limited = phase2Results["429"] || 0;
  console.log("");
  if (p2Limited > 0) {
    console.log(`RESULT: Sustained rate limiting CONFIRMED — ${p2Limited} requests throttled.`);
  } else {
    console.log("RESULT: No throttling at sustained rate.");
  }

  console.log("================================================");
}

main();
