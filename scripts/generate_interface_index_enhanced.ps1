$project='E:\overgame\project_IDEA2'
$controllerRoot=Join-Path $project 'SpringBoot-backeng\src\main\java\com\example\springbootbackeng\controller'
$serviceRoot=Join-Path $project 'SpringBoot-backeng\src\main\java\com\example\springbootbackeng\service\impl'
$mapperRoot=Join-Path $project 'SpringBoot-backeng\src\main\java\com\example\springbootbackeng\mapper'

$svcDict=@{}
Get-ChildItem $serviceRoot -Filter '*ServiceImpl.java' | ForEach-Object {
  $txt=Get-Content $_.FullName
  $service='';$impl='';$baseMapper='';$baseEntity=''
  foreach($line in $txt){
    if($line -match 'class\s+([A-Za-z0-9_]+)\s+(extends\s+ServiceImpl<\s*([A-Za-z0-9_]+)\s*,\s*([A-Za-z0-9_]+)\s*>\s+)?implements\s+([A-Za-z0-9_]+)'){
      $impl=$matches[1];$baseMapper=$matches[3];$baseEntity=$matches[4];$service=$matches[5];break
    }
  }
  $mappers=@()
  foreach($line in $txt){
    if($line -match 'private\s+final\s+([A-Za-z0-9_]+Mapper)\s+[A-Za-z0-9_]+\s*;'){$mappers+=$matches[1]}
    elseif($line -match 'private\s+([A-Za-z0-9_]+Mapper)\s+[A-Za-z0-9_]+\s*;'){$mappers+=$matches[1]}
  }
  if($baseMapper){$mappers+=$baseMapper}
  $mappers=$mappers|Select-Object -Unique
  if($service){$svcDict[$service]=[PSCustomObject]@{impl=$impl;mappers=$mappers;baseEntity=$baseEntity}}
}

$mapperEntity=@{}
Get-ChildItem $mapperRoot -Filter '*Mapper.java' | ForEach-Object {
  $txt=Get-Content $_.FullName
  $entity=''
  foreach($line in $txt){
    if($line -match 'interface\s+[A-Za-z0-9_]+\s+extends\s+BaseMapper<\s*([A-Za-z0-9_]+)\s*>'){ $entity=$matches[1]; break }
  }
  $mapperEntity[$_.BaseName]=$entity
}

function Get-Module($controller){
  switch -Regex ($controller) {
    'Login|UserSecurity' { 'AuthAndAccountSecurity'; break }
    '^UserController$' { 'UserAndPermission'; break }
    'MaterialDemand|ReviewLog' { 'MaterialDemandAndApproval'; break }
    '^MaterialController$' { 'MaterialBase'; break }
    'AllocationPlan' { 'AllocationPlan'; break }
    'TransportRecord' { 'Transport'; break }
    'Inventory(Log)?Controller' { 'Inventory'; break }
    'Team|Supplier|Vehicle' { 'MasterData'; break }
    'Message' { 'MessageCenter'; break }
    'OperationLog' { 'OperationAudit'; break }
    'AiRecognition|AiAnalysis' { 'AIModule'; break }
    'CacheMonitor|Stats|Hello' { 'MonitoringAndStats'; break }
    default { 'Other' }
  }
}

$rows=@()
Get-ChildItem $controllerRoot -Filter '*Controller.java' | Sort-Object Name | ForEach-Object {
  $file=$_; $lines=Get-Content $file.FullName; $controller=$file.BaseName
  $classBase=''
  for($i=0;$i -lt $lines.Count;$i++){
    if($lines[$i] -match '@RequestMapping\("([^"]*)"\)'){ $classBase=$matches[1]; break }
  }
  $svcVars=@{}
  for($i=0;$i -lt $lines.Count;$i++){
    if($lines[$i] -match 'private\s+(final\s+)?([A-Za-z0-9_]+Service)\s+([A-Za-z0-9_]+)\s*;'){ $svcVars[$matches[3]]=$matches[2] }
  }
  for($i=0;$i -lt $lines.Count;$i++){
    $http='';$sub=''
    if($lines[$i] -match '@GetMapping\("?([^"\)]*)"?\)'){ $http='GET';$sub=$matches[1] }
    elseif($lines[$i] -match '@PostMapping\("?([^"\)]*)"?\)'){ $http='POST';$sub=$matches[1] }
    elseif($lines[$i] -match '@PutMapping\("?([^"\)]*)"?\)'){ $http='PUT';$sub=$matches[1] }
    elseif($lines[$i] -match '@DeleteMapping\("?([^"\)]*)"?\)'){ $http='DELETE';$sub=$matches[1] }
    elseif($lines[$i] -match '@PatchMapping\("?([^"\)]*)"?\)'){ $http='PATCH';$sub=$matches[1] }
    elseif($lines[$i] -match '@RequestMapping\(\s*value\s*=\s*"([^"]*)"\s*,\s*method\s*=\s*RequestMethod\.([A-Z]+)'){ $http=$matches[2];$sub=$matches[1] }
    if($http -eq ''){ continue }

    $j=$i+1
    while($j -lt $lines.Count -and ($lines[$j].Trim() -eq '' -or $lines[$j].Trim().StartsWith('@'))){ $j++ }
    $sig=$lines[$j].Trim(); $mname=''
    if($sig -match '\s([A-Za-z0-9_]+)\s*\('){ $mname=$matches[1] }

    $bodyStart=$j; while($bodyStart -lt $lines.Count -and -not ($lines[$bodyStart] -match '\{')){ $bodyStart++ }
    $depth=0; $calls=@()
    for($k=$bodyStart;$k -lt $lines.Count;$k++){
      $line=$lines[$k]
      $depth += ([regex]::Matches($line,'\{')).Count
      $depth -= ([regex]::Matches($line,'\}')).Count
      foreach($var in $svcVars.Keys){
        if($line -match ('\b'+[regex]::Escape($var)+'\.([A-Za-z0-9_]+)\s*\(')){
          $calls += ($svcVars[$var]+'.'+$matches[1]+'()')
        }
      }
      if($k -gt $bodyStart -and $depth -le 0){ break }
    }
    $calls=$calls|Select-Object -Unique
    $svcNames=@($calls|ForEach-Object{ if($_ -match '^([A-Za-z0-9_]+)\.'){$matches[1]} })|Select-Object -Unique
    $impls=@();$mappers=@();$entities=@()
    foreach($sn in $svcNames){
      if($svcDict.ContainsKey($sn)){
        $impls += $svcDict[$sn].impl
        foreach($m in $svcDict[$sn].mappers){
          if($m){ $mappers += $m; if($mapperEntity.ContainsKey($m) -and $mapperEntity[$m]){ $entities += $mapperEntity[$m] } }
        }
        if($svcDict[$sn].baseEntity){ $entities += $svcDict[$sn].baseEntity }
      }
    }
    $impls=$impls|Select-Object -Unique; $mappers=$mappers|Select-Object -Unique; $entities=$entities|Select-Object -Unique

    $path=$classBase
    if($sub -and $sub -ne ''){ if($sub.StartsWith('/')){$path=$classBase+$sub}else{$path=$classBase+'/'+$sub} }
    if($path -eq ''){$path='/'}
    $rel='SpringBoot-backeng/src/main/java/com/example/springbootbackeng/controller/'+$file.Name
    $rows += [PSCustomObject]@{
      module=(Get-Module $controller)
      controller=$controller
      api=($http+' '+$path)
      pathOnly=$path
      method=($controller+'.'+$mname+'()')
      serviceCalls=($calls -join '<br>')
      serviceImpl=($impls -join ', ')
      mapper=($mappers -join ', ')
      entity=($entities -join ', ')
      sourceLink=('['+$mname+'()'+']('+$rel+'#L'+($j+1)+')')
    }
  }
}

$rows = $rows | Sort-Object controller, pathOnly, api
$out=Join-Path $project 'INTERFACE_CALL_CHAIN_INDEX_ENHANCED.md'
$md=@()
$md += '# Interface Call Chain Index (Enhanced)'
$md += ''
$md += '> Grouped by Controller, sorted by path within each group, with source line links.'
$md += ''
$grouped = $rows | Group-Object controller
foreach($g in $grouped){
  $module = ($g.Group | Select-Object -First 1).module
  $ctrlName=$g.Name
  $ctrlFile='SpringBoot-backeng/src/main/java/com/example/springbootbackeng/controller/'+$ctrlName+'.java'
  $md += '## '+$ctrlName+' ('+$module+')'
  $md += ''
  $md += '- Controller source: ['+$ctrlFile+']('+$ctrlFile+')'
  $md += ''
  $md += '| Path | API | Controller Method | Direct Service Calls | ServiceImpl | Mapper | Entity | Source Link |'
  $md += '|---|---|---|---|---|---|---|---|'
  foreach($r in ($g.Group | Sort-Object pathOnly, api)){
    $svc = ($r.serviceCalls -replace '\|','/')
    $impl = ($r.serviceImpl -replace '\|','/')
    $mp = ($r.mapper -replace '\|','/')
    $en = ($r.entity -replace '\|','/')
    $md += ('| '+$r.pathOnly+' | '+$r.api+' | '+$r.method+' | '+$svc+' | '+$impl+' | '+$mp+' | '+$en+' | '+$r.sourceLink+' |')
  }
  $md += ''
}
$md -join "`r`n" | Set-Content -Path $out -Encoding UTF8
Write-Output ('WROTE '+$out+' ROWS='+$rows.Count+' CONTROLLERS='+$grouped.Count)
