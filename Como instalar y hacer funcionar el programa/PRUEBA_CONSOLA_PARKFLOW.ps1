# ParkFlow: demostracion por consola (PowerShell 5.1)
# Crea piso, zona, espacio, entrada, liquidacion, pago en efectivo y cierre.
# Solo usa las APIs; no escribe directamente en PostgreSQL.
# Ejecutar desde la carpeta backend con el stack Docker encendido.

$ErrorActionPreference = 'Stop'

$identityUrl = 'http://localhost:8081'
$parkingUrl = 'http://localhost:8080'
$billingUrl = 'http://localhost:8083'

function Invoke-ParkFlowApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body,
        [string]$IdempotencyKey
    )

    $headers = @{ Authorization = "Bearer $script:accessToken" }
    if ($IdempotencyKey) { $headers['Idempotency-Key'] = $IdempotencyKey }

    $request = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $headers
        ContentType = 'application/json'
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) { $request.Body = ConvertTo-Json -InputObject $Body -Depth 8 }
    Invoke-RestMethod @request
}

Write-Host 'Comprobando que Billing tenga gracia en 0 para probar un pago inmediato...'
$graceLine = docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' parkflow-billing 2>$null |
    Where-Object { $_ -match '^PARKFLOW_GRACE_MINUTES=' } | Select-Object -First 1
if (-not $graceLine) {
    throw 'No pude leer PARKFLOW_GRACE_MINUTES del contenedor parkflow-billing. Verifica que Docker este arriba.'
}
$graceMinutes = ($graceLine -replace '^PARKFLOW_GRACE_MINUTES=', '').Trim()
if ($graceMinutes -ne '0') {
    throw "Billing tiene $graceMinutes minutos de gracia. Para que la prueba cobre ahora, cambia temporalmente PARKFLOW_GRACE_MINUTES=0 en .env.parkflow y recrea Billing. Instrucciones al final de este archivo. No se crearon datos."
}

$email = Read-Host 'Correo del administrador (ejemplo: admin@parkflow.test)'
$securePassword = Read-Host 'Contrasena' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}

Write-Host 'Iniciando sesion en Identity...'
$loginBody = @{ email = $email; password = $plainPassword } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$identityUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
$script:accessToken = $login.accessToken
if (-not $script:accessToken) { throw 'Identity no devolvio un token de acceso.' }
Write-Host ("Sesion iniciada: {0} | Roles: {1}" -f $login.usuario.email, ($login.usuario.roles -join ', '))
$plainPassword = $null

$stamp = Get-Date -Format 'ddHHmmss'
$floorCode = "T$stamp"
$zoneCode = "Z$stamp"
$spaceCode = "C$stamp"
$plate = "T$stamp"
$entryKey = [guid]::NewGuid().ToString()
$exitKey = [guid]::NewGuid().ToString()
$cashKey = [guid]::NewGuid().ToString()

Write-Host 'Buscando un nivel disponible y creando piso, zona y espacio de prueba...'
$floors = @(Invoke-ParkFlowApi -Method GET -Url "$parkingUrl/api/v1/pisos")
$usedOrders = @($floors | ForEach-Object { [int]$_.orden })
$floorOrder = $null
foreach ($candidate in @(0, -1, -2, -3, -4, -5)) {
    if ($usedOrders -notcontains $candidate) { $floorOrder = $candidate; break }
}
if ($null -eq $floorOrder) {
    throw 'No hay un nivel disponible entre 0 y -5. Libera o deshabilita un piso antes de repetir la demostracion.'
}

$floor = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/v1/pisos" -Body @{
    codigo = $floorCode
    nombre = "Piso de prueba $stamp"
    orden = $floorOrder
}
$zone = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/zonas" -Body @{
    pisoId = $floor.id
    codigo = $zoneCode
    nombre = "Zona de prueba $stamp"
    descripcion = 'Creada por el guion de pruebas de consola'
}
$zone = Invoke-ParkFlowApi -Method PUT -Url "$parkingUrl/api/zonas/$($zone.id)" -Body @{
    pisoId = $floor.id
    codigo = $zoneCode
    nombre = "Zona editada $stamp"
    descripcion = 'Prueba de actualizacion por API'
}
$space = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/v1/espacios" -Body @{
    zonaId = $zone.id
    codigo = $spaceCode
    tipo = 'CARRO'
}
Write-Host ("Piso={0} | Zona={1} | Espacio={2}" -f $floor.codigo, $zone.codigo, $space.codigo)

Write-Host 'Abriendo una caja de prueba con saldo inicial de $100.000...'
$cashRegister = Invoke-ParkFlowApi -Method POST -Url "$billingUrl/api/billing/cajas" -Body @{
    codigo = "CJ$stamp"
    nombre = "Caja de prueba $stamp"
    descripcion = 'Caja temporal para una demostracion por consola'
}
$cashSession = Invoke-ParkFlowApi -Method POST -Url "$billingUrl/api/billing/cajas/sesiones" -IdempotencyKey $cashKey -Body @{
    cajaId = $cashRegister.id
    saldoInicial = [decimal]100000.00
    observacion = 'Apertura para prueba de consola'
}

Write-Host 'Registrando entrada de un carro...'
$entry = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/sesiones/entradas" -IdempotencyKey $entryKey -Body @{
    placa = $plate
    tipoVehiculo = 'CARRO'
    espacioId = $space.id
}
Write-Host ("Entrada creada | Ticket={0} | Placa={1} | Estado={2}" -f $entry.numeroTicket, $entry.placa, $entry.estado)
Start-Sleep -Seconds 2

Write-Host 'Solicitando salida. Parking consultara Billing y creara la liquidacion...'
$firstExit = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/sesiones/$([uri]::EscapeDataString($entry.numeroTicket))/salida" -IdempotencyKey $exitKey
Write-Host ("Liquidacion={0} | Total={1} | Estado={2} | Salida confirmada={3}" -f $firstExit.liquidacionId, $firstExit.total, $firstExit.estadoLiquidacion, $firstExit.salidaConfirmada)
if ($firstExit.estadoLiquidacion -ne 'PENDIENTE') {
    throw "La liquidacion no quedo PENDIENTE (estado: $($firstExit.estadoLiquidacion)). No se registrara un pago. Revisa que Billing este con gracia 0 y vuelve a empezar con otra ejecucion del guion."
}

Write-Host 'Consultando la liquidacion y el pago pendiente en Billing...'
$liquidation = Invoke-ParkFlowApi -Method GET -Url "$billingUrl/internal/v1/liquidaciones/by-idempotency-key/$exitKey"
if (-not $liquidation.pago -or -not $liquidation.pago.id) { throw 'Billing no devolvio el pago pendiente.' }

Write-Host 'Confirmando el efectivo recibido en Billing...'
$payment = Invoke-ParkFlowApi -Method POST -Url "$billingUrl/internal/v1/pagos/$($liquidation.pago.id)/resolver" -Body @{
    metodo = 'EFECTIVO'
    sesionCajaId = $cashSession.id
}
Write-Host ("Pago={0} | Estado={1} | Monto={2}" -f $payment.id, $payment.estado, $payment.monto)

Write-Host 'Confirmando la salida en Parking con la misma clave de operacion...'
$finalExit = Invoke-ParkFlowApi -Method POST -Url "$parkingUrl/api/sesiones/$([uri]::EscapeDataString($entry.numeroTicket))/salida" -IdempotencyKey $exitKey
if (-not $finalExit.salidaConfirmada) { throw 'Parking aun no confirma la salida. No cierres la caja; revisa la liquidacion y los logs.' }

Write-Host 'Consultando estado final y comprobante...'
$finalLiquidation = Invoke-ParkFlowApi -Method GET -Url "$billingUrl/internal/v1/liquidaciones/$($liquidation.id)"
$finalSession = Invoke-ParkFlowApi -Method GET -Url "$parkingUrl/api/sesiones/$([uri]::EscapeDataString($entry.numeroTicket))"
$spacesAfterExit = @(Invoke-ParkFlowApi -Method GET -Url "$parkingUrl/api/v1/espacios?pisoId=$($floor.id)")
$testSpace = $spacesAfterExit | Where-Object { $_.id -eq $space.id } | Select-Object -First 1

Write-Host 'Cerrando la caja de prueba con el saldo esperado...'
$cashBeforeClose = Invoke-ParkFlowApi -Method GET -Url "$billingUrl/api/billing/cajas/sesiones/$($cashSession.id)"
$closedCash = Invoke-ParkFlowApi -Method POST -Url "$billingUrl/api/billing/cajas/sesiones/$($cashSession.id)/cerrar" -Body @{
    saldoContado = [decimal]$cashBeforeClose.saldoEsperado
    observacion = 'Cierre de caja de prueba; efectivo contado igual al esperado'
}

Write-Host 'Probando baja logica y restauracion del piso...'
$disabledFloor = Invoke-ParkFlowApi -Method PATCH -Url "$parkingUrl/api/v1/pisos/$($floor.id)/habilitado" -Body @{ habilitado = $false }
$restoredFloor = Invoke-ParkFlowApi -Method PATCH -Url "$parkingUrl/api/v1/pisos/$($floor.id)/habilitado" -Body @{ habilitado = $true }

Write-Host ''
Write-Host '===== RESUMEN DE LA PRUEBA ====='
Write-Host ("Ticket: {0} | Sesion: {1}" -f $finalSession.numeroTicket, $finalSession.estado)
Write-Host ("Liquidacion: {0} | Total: {1} | Comprobante: {2}" -f $finalLiquidation.estado, $finalLiquidation.total, $finalLiquidation.comprobante.numero)
Write-Host ("Espacio {0}: disponible={1}" -f $space.codigo, $testSpace.disponible)
Write-Host ("Caja: {0} | Diferencia de arqueo: {1}" -f $closedCash.estado, $closedCash.diferencia)
Write-Host ("Piso restaurado y habilitado: {0}" -f $restoredFloor.habilitado)
Write-Host ("IDs para consultas: Piso={0} Zona={1} Espacio={2} Caja={3}" -f $floor.id, $zone.id, $space.id, $cashSession.id)
Write-Host 'La prueba termino. Las sesiones, pagos, comprobantes y movimientos quedan guardados para auditoria.'

<#+
PREPARACION PARA HACER UN COBRO INMEDIATO (solo entorno local de pruebas)

1. En .env.parkflow cambia temporalmente:
   PARKFLOW_GRACE_MINUTES=0
2. Desde backend recrea Billing (no borra PostgreSQL ni sus datos):
   docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml up -d --force-recreate billing
3. Guarda este archivo y ejecutalo desde PowerShell:
   & ".\Como instalar y hacer funcionar el programa\PRUEBA_CONSOLA_PARKFLOW.ps1"
4. Al terminar, restaura en .env.parkflow:
   PARKFLOW_GRACE_MINUTES=15
5. Aplica de nuevo la configuracion normal:
   docker compose --env-file .env.parkflow -f docker-compose.parkflow.yml up -d --force-recreate billing

Con 15 minutos de gracia, una salida inmediata queda GRATUITA y no se puede demostrar el cobro.
El guion crea registros auditables; no borra sesiones, pagos ni comprobantes. Para volver a probar,
ejecutalo otra vez: genera codigos y claves nuevos.

ParkFlow conserva sesiones, pagos y comprobantes para auditoria. Por eso no existe DELETE fisico
para las sesiones del parqueo; los pisos se deshabilitan y pueden restaurarse en vez de borrarse.
#>
