$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '..\..\ai-chat-server')
$env:SERVER_PORT = '18081'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:mem:e2e;MODE=MySQL;DB_CLOSE_DELAY=-1'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
$env:DEEPSEEK_API_KEY = 'local-test-provider-only'
$env:DEEPSEEK_BASE_URL = 'http://127.0.0.1:18090'
$env:DEEPSEEK_MODEL = 'local-test-model'
$env:ADMIN_EMAIL = 'admin@e2e.local'
$env:ADMIN_PASSWORD = 'E2eAdminPassword123!'
$env:SPRING_PROFILES_ACTIVE = 'default'
& mvn.cmd -q spring-boot:run
exit $LASTEXITCODE
