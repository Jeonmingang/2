# SamSkyBridge-WhiteBarrier
- Java 8, Spigot/Paper 1.16.5
- Depends on BentoBox (BSkyBlock). Set `${bentobox.version}` in `pom.xml` to match your server.
- Features:
  - Island size upgrade (protection radius + visual white "barrier" using white REDSTONE particles)
  - Shows border when player enters island; clears when leaves
  - Auto-upgrade on IslandLevelEvent based on `upgrade.yml` thresholds
- Commands: `/ssb reload`

## Build
mvn -DskipTests clean package

## Configure
See `upgrade.yml` for radius per tier, `settings.yml` for particle density/period.


## New Features
- `/upgrades` GUI: Island Size & Team Size upgrades (config in `upgrade.yml`).
- Team size upgrades use BentoBox per-island override (`setMaxMembers` for MEMBER_RANK).
- `/rank top|me|recalc`: Ranking by island block value, including Pixelmon blocks if exposed via namespaced keys.


## 초간단 빌드 (Maven Wrapper)
**Java 8만 설치**되어 있으면 됩니다. Maven은 자동으로 내려받습니다.

### Windows
1) `빌드_윈도우.bat` 더블클릭
2) 완료 후 `target/SamSkyBridge-0.2.0.jar` 생성

### macOS / Linux
```bash
chmod +x build_mac_linux.sh
./build_mac_linux.sh
```
완료 후 `target/SamSkyBridge-0.2.0.jar` 생성
