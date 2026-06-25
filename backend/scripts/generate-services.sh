#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

services=(
  "tenant-service|tenant|Tenant|tenant lifecycle,subscriptions,isolation"
  "billing-service|billing|Billing|plans,usage,invoices"
  "tenant-config-service|tenantconfig|TenantConfig|branding,security policies,integrations"
  "feature-flag-service|featureflag|FeatureFlag|flags,variants,rollout"
  "customer-service|customer|Customer|customers,risk profiles,customer debts"
  "debt-service|debt|Debt|debts,balances,status history"
  "contract-service|contract|Contract|contracts,credit balances,status"
  "cheque-service|cheque|Cheque|cheques,unpaid incidents,status"
  "guarantee-service|guarantee|Guarantee|guarantees,valuations,enforcement"
  "organization-service|organization|Organization|organizations,agents,skills,capacity"
  "case-service|casefile|Case|cases,actions,notes,timeline"
  "segmentation-service|segmentation|Segmentation|DMN decisions,simulation,decision traces"
  "assignment-service|assignment|Assignment|eligibility,capacity,assignment decisions"
  "workflow-service|workflow|Workflow|BPMN processes,tasks,timers,incidents"
  "notification-service|notification|Notification|templates,delivery attempts,channels"
  "payment-service|payment|Payment|payments,promises,plans,reconciliation"
  "legal-service|legal|Legal|legal cases,court events,enforcement"
  "agency-service|agency|Agency|external agencies,portfolios,SLA"
  "document-service|document|Document|documents,versions,retention"
  "kpi-service|kpi|Kpi|metrics,dashboards,reports"
  "scoring-service|scoring|Scoring|risk scores,model versions,explanations"
  "audit-service|audit|Audit|immutable audit,search,exports"
  "configuration-service|configuration|Configuration|versioned configuration,SLA,channels"
  "reference-data-service|referencedata|ReferenceData|reference domains,values,versions"
  "data-contract-service|datacontract|DataContract|schema versions,field definitions,validation"
)

make_module() {
  local base="$1"
  local artifact="$2"
  local package_suffix="$3"
  local class_name="$4"
  local capabilities="$5"
  local port="$6"
  local extra_dependencies="${7:-}"
  local package_dir="$base/src/main/java/com/deptcollector/$package_suffix"

  mkdir -p "$package_dir" "$base/src/main/resources/db/migration" "$base/src/test/java/com/deptcollector/$package_suffix"

  cat > "$base/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.deptcollector</groupId>
    <artifactId>dept-collector-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>
  <artifactId>$artifact</artifactId>
  <dependencies>
    <dependency>
      <groupId>com.deptcollector</groupId>
      <artifactId>shared-kernel</artifactId>
      <version>\${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
$extra_dependencies
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
EOF

  cat > "$package_dir/${class_name}ServiceApplication.java" <<EOF
package com.deptcollector.$package_suffix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class ${class_name}ServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(${class_name}ServiceApplication.class, args);
    }
}
EOF

  cat > "$base/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: $artifact
  datasource:
    url: \${DB_URL:jdbc:postgresql://localhost:5432/${artifact//-/_}}
    username: \${DB_USER:collector}
    password: \${DB_PASSWORD:collector}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: \${OIDC_JWK_SET_URI:http://localhost:8180/realms/dept-collector/protocol/openid-connect/certs}

server:
  port: \${SERVER_PORT:$port}
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      probes:
        enabled: true

app:
  bounded-context: $package_suffix
  capabilities: $capabilities
  security:
    enabled: \${SECURITY_ENABLED:false}

logging:
  pattern:
    level: "%5p [\${spring.application.name:},%X{correlationId:-}]"
EOF

  cat > "$base/src/main/resources/db/migration/V1__baseline.sql" <<EOF
CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;
EOF

  cat > "$base/src/test/java/com/deptcollector/$package_suffix/${class_name}ServiceApplicationTest.java" <<EOF
package com.deptcollector.$package_suffix;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ${class_name}ServiceApplicationTest {
    @Test
    void applicationClassExists() {
        assertThat(${class_name}ServiceApplication.class).isNotNull();
    }
}
EOF
}

port=8081
for definition in "${services[@]}"; do
  IFS='|' read -r artifact package_suffix class_name capabilities <<< "$definition"
  extra=""
  if [[ "$artifact" == "segmentation-service" ]]; then
    extra='    <dependency>
      <groupId>org.flowable</groupId>
      <artifactId>flowable-spring-boot-starter-dmn</artifactId>
      <version>${flowable.version}</version>
    </dependency>'
  elif [[ "$artifact" == "workflow-service" ]]; then
    extra='    <dependency>
      <groupId>org.flowable</groupId>
      <artifactId>flowable-spring-boot-starter-process</artifactId>
      <version>${flowable.version}</version>
    </dependency>'
  fi
  make_module "$ROOT/services/$artifact" "$artifact" "$package_suffix" "$class_name" "$capabilities" "$port" "$extra"
  port=$((port + 1))
done

make_module "$ROOT/api-gateway" "api-gateway" "gateway" "ApiGateway" "routing,tenant resolution,rate limiting" "8080"
sed -i.bak 's#<relativePath>../../pom.xml</relativePath>#<relativePath>../pom.xml</relativePath>#' "$ROOT/api-gateway/pom.xml"
rm -f "$ROOT/api-gateway/pom.xml.bak"
