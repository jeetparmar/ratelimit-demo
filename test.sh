#!/usr/bin/env bash
# Fires 25 quick requests; first ~10 pass (burst), the rest get 429 until tokens refill.
for i in $(seq 1 25); do
  curl -s -o /dev/null -w "$i -> %{http_code}\n" http://localhost:8080/api/hello
done
