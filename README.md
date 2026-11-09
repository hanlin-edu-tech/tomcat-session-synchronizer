## 使用情況調查（2026-11-09）

> 本節為 hanlin-edu-tech repo 清理作業的調查紀錄，待負責人確認後決定封存或刪除。

- 調查結論：仍在使用
- 建議處置：保留，理由：本 repo 發佈的 `tw.com.ehanlin:tomcat-session-synchronizer-*` 函式庫被多個正式機運行中、且近期仍有開發的服務依賴。
- 最後更新：pushedAt 2022-11-30；最後 commit `37035a4` 2021-10-26 AaronChuang 更新mongo db lib (#1)（預設分支 `main`）；`teams-103` 分支最後 commit `080ef66` 2022-11-30 hotdog929 幫 teams 特製的 session manager
- 用途：Tomcat 8 session 同步函式庫，模組 `core`、`memory`、`mongodb`、`tomcat8`（`teams-103` 分支另有 `teams`），發佈為 `tw.com.ehanlin:tomcat-session-synchronizer-{core,memory,mongodb,tomcat8}` 到公司 Maven 倉庫（GCS），由 Cloud Build 依 tag 執行 `./gradlew publish`。

### 檢查結果

| 檢查項目 | 結果 | 依據 |
|---|---|---|
| GitHub 活動 | 2022-11-30 後無 commit；非 fork | 分支 2 個：`main`（2021-10-26）、`teams-103`（2022-11-30）；PR 1 個（#1 2021-10-26 merged）；issue 0；Actions workflow 0、執行 0；release 0；tag 7 個（最新 v1.0.1、v1.0.0-teams103）；webhook 1 個（見待確認事項） |
| org 內程式碼引用 | 有仍在使用的 repo 依賴 | `gh search code --owner hanlin-edu-tech tomcat-session-synchronizer` 命中 16 個 repo 的 build 設定：tutor-platform-root、tutor-platform-products、tutor-platform-my-course、tutor-platform-keys、tutor-platform-user、tutor-platform-cart、tutor-platform-order、tutor-currency-mission、tutor-questionnaire、tutor-guardian_104、tutor-ms_message、tutor-ms-user-status、tutor-nu-ehanlin、teams-personal-test-report-bg、event-IRT-107、micronaut-sample；其中 tutor-platform-root（pushedAt 2026-11-04）、tutor-platform-products、tutor-platform-my-course（pushedAt 2026-10-28）在正式機運行且 2026-11-09 仍有請求。細節記錄於內部調查報告 |
| GCP 部署 | 函式庫發佈管道存在，依賴方服務運行中 | 已查核，細節記錄於內部調查報告（Maven 倉庫最新版本 1.0.1 於 2021-10-26 發佈、1.0.0-teams103 於 2022-11-30 發佈；發佈用 Cloud Build trigger 2 個仍存在） |
| AWS 部署 | 查無 | 已查核 4 個 AWS 帳號的 Lambda、REST／HTTP API Gateway、ECR、ECS、EventBridge、SQS、S3、CloudFront 盤點，以 `session-synchronizer`、`tomcat-session` 搜尋查無 |

### 待確認事項
- repo webhook 指向 Travis CI 的 travis-ci.org 通知端點（2019-01-08 建立，GitHub 顯示狀態 unused；2026-11-09 以 HTTPS 連線該端點逾時無回應），預設分支已無 `.travis.yml`，建議移除。

# tomcat-session-synchronizer

## 使用方法
```groovy
repositories {
    maven { url "gcs://ehanlin-java-repo/maven2" }
}

dependencies {
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-core:1.0.0"
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-tomcat8:1.0.0"
    implementation "tw.com.ehanlin:tomcat-session-synchronizer-mongodb:1.0.0"
}
```

## 佈署
```shell
./gradlew -Pversion=1.0.0 publish
```