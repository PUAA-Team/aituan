#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "[integration-api] Running controller, API, contract, migration, and boundary tests"
mvn -B -f "${repo_root}/services/pom.xml" \
  -pl api-gateway,identity-asset-service,merchant-catalog-service,trade-fulfillment-service,engagement-platform-service \
  -am \
  '-Dtest=*ControllerTest,*ApiIntegrationTest,*IntegrationTest,*ContractTest,*MigrationTest,*MigrationSmokeTest,*MysqlSmokeTest,*ApplicationTest,PublicEndpointInventoryTest,ServiceBoundaryTest,SupportRepositoryTest,PlatformRemoteClientTest' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  clean verify
