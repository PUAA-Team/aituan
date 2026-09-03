#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "[unit] Running isolated unit tests for Gateway and A/B/C/D services"
mvn -B -f "${repo_root}/services/pom.xml" \
  -pl api-gateway,identity-asset-service,merchant-catalog-service,trade-fulfillment-service,engagement-platform-service \
  -am \
  -Dtest=GatewaySecurityFilterTest,InternalIdentityServiceTest,MerchantCatalogServiceUnitTest,InternalTradeServiceUnitTest,FileStorageServiceTest,StationMessageClientTest,ReviewOrderMarkCompensationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  clean verify
