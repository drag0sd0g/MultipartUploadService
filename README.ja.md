[English](README.md)

# シンプルなファイルストレージサーバー&CLI

[![Clean Build](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/gradle.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/codeql.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/codeql.yml)
[![Docker Build](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/docker-publish.yml)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Quarkus Version](https://img.shields.io/badge/Quarkus-3.17.4-blue.svg)](https://quarkus.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Quarkus と Java 21 で構築された、モダンでコンテナ化されたファイルストレージサービス。REST API を提供します。

## 📋 目次

1. [機能](#機能)
2. [前提条件](#前提条件)
3. [Docker でのクイックスタート](#docker-でのクイックスタート)
4. [ビルドとパッケージ方法](#ビルドとパッケージ方法)
5. [サーバーの起動](#サーバーの起動)
6. [クライアントの実行](#クライアントの実行)
7. [オブザーバビリティ](#オブザーバビリティ)
8. [開発](#開発)
9. [テスト](#テスト)

---

## ✨ 機能

- 🚀 **モダンなスタック**: Quarkus 3.17.4 と Java 21 で構築
- 🐳 **コンテナ化**: サーバーとクライアントの Docker イメージ提供
- 📊 **オブザーバビリティ**: Prometheus メトリクスと構造化ログ
- 🔒 **セキュリティ**: CodeQL スキャンと Dependabot による依存関係更新
- ✅ **品質管理**: Checkstyle、PMD、SpotBugs による静的解析
- 🧪 **テスト済み**: 80% コードカバレッジ + 包括的な統合テスト
- 📖 **API ドキュメント**: OpenAPI/Swagger UI 同梱
- 🌐 **REST API**: シンプルで分かりやすいファイルストレージ API

---

## 前提条件

### Docker で実行する場合（推奨）
- Docker 20.10 以上
- Docker Compose 2.0 以上

### ソースからビルドする場合
- JDK 21 以降（[Adoptium](https://adoptium.net/) からダウンロード）
- JAVA_HOME 環境変数を JDK インストール先に設定

---

## 🚀 Docker でのクイックスタート

最も簡単な方法は Docker Compose を使用することです：

```bash
# リポジトリをクローン
git clone https://github.com/drag0sd0g/MultipartUploadService.git
cd MultipartUploadService

# サーバーとクライアントを起動
docker-compose up -d

# サーバーのヘルスチェック
curl http://localhost:8080/q/health

# ログを表示
docker-compose logs -f server

# サービスを停止
docker-compose down
```

サーバーは http://localhost:8080 で利用可能になり、以下のエンドポイントが提供されます：
- REST API: http://localhost:8080/v1/files
- Swagger UI: http://localhost:8080/q/swagger-ui/
- Prometheus メトリクス: http://localhost:8080/q/metrics
- ヘルスチェック: http://localhost:8080/q/health

---

## ビルドとパッケージ方法

リポジトリのルートディレクトリで次のコマンドを実行し、クライアント・サーバー両方をビルドします。

### Windows の場合

```bat
gradlew clean build distClient distServer
```

### Unix の場合

```bash
chmod +x gradlew && ./gradlew clean build distClient distServer
```

クライアントとサーバーの jar は _build_ 配下の _fsclient/_ および _fsserver/_ フォルダに展開されます。

### Docker イメージのビルド

```bash
# サーバーイメージをビルド
docker build -f file-storage-server/Dockerfile -t file-storage-server:latest .

# クライアントイメージをビルド
docker build -f file-storage-client/Dockerfile -t file-storage-client:latest .
```

---

## サーバーの起動

### Docker を使用（推奨）

```bash
docker run -p 8080:8080 -v $(pwd)/data-server:/app/data-server file-storage-server:latest
```

### Java を使用

_build/fsserver_ に移動し、以下のコマンドでサーバーを起動します：

```shell
java -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

プロパティは _src/main/resources_ 配下の _application.properties_ に設定されていますが、Quarkus の多数のデフォルト値も利用されます。**ポート**や**ホスト**を上書きしたい場合は、_jar_ 実行時に _-D_ 引数で指定してください：

```shell
java -Dquarkus.http.host="192.168.11.7" -Dquarkus.http.port=8085 -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

### サーバーノート

- デフォルトで http://127.0.0.1:8080 で起動
- REST API ドキュメントは http://127.0.0.1:8080/q/swagger-ui/ で利用可能
- OpenAPI 仕様は http://127.0.0.1:8080/q/openapi で利用可能
- Quarkus はマルチパートアップロードファイルを一時領域に保存し、その後永続ストレージフォルダ（_data-server_）にコピー。リクエスト処理後、一時ファイルは自動削除されます
- 全体の容量制限はありませんが、各ファイル 10MB のサイズ上限があります
- この設定は _quarkus.http.limits.max-form-attribute-size_ で管理され、超過した場合は HTTP 413 が返されます
  （CLI からも **GET** _/v1/stats/fileUploadSizeLimit_ でこの値を取得可能）
- REST API は現在 **/v1** バージョンです

---

## クライアントの実行

### Docker を使用

```bash
# ファイル一覧
docker run --network host file-storage-client:latest -l

# ファイルをアップロード
docker run --network host -v $(pwd):/data file-storage-client:latest -u /data/myfile.txt

# ファイルを削除
docker run --network host file-storage-client:latest -d myfile.txt
```

### Java を使用

別のコマンドラインで _build/fsclient_ に移動し、以下の 3 通りからコマンドを選択します：

### アップロード済みファイルの一覧

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --list-files
```

または

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### ファイルをアップロード

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --upload-file <ファイルパス>
```

または

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -u <ファイルパス>
```

### アップロード済みファイルを削除

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --delete-file <ファイル名>
```

または

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -d <ファイル名>
```

削除時はファイル名のみで OK。パス不要です。

デフォルトでは http://127.0.0.1:8080 のサーバーへ接続しますが、必要なら CLI 起動時に _-Dfsserver.api.rootUrl_ で明示的に上書き可能です：

```shell
java -Dfsserver.api.rootUrl="http://192.168.11.7:8085" -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### クライアントノート

- コマンドラインオプション/引数が間違っているか不足していれば、CLI は終了し、Usage ガイドを表示します

---

## 📊 オブザーバビリティ

### Prometheus メトリクス

サーバーは `/q/metrics` で Prometheus 互換のメトリクスを公開します：

```bash
curl http://localhost:8080/q/metrics
```

メトリクスには以下が含まれます：
- JVM メトリクス（メモリ、GC、スレッド）
- HTTP サーバーメトリクス（リクエスト、レスポンスタイム）
- システムメトリクス（CPU、ファイルディスクリプタ）

### ヘルスチェック

```bash
# 全体のヘルス
curl http://localhost:8080/q/health

# Liveness プローブ
curl http://localhost:8080/q/health/live

# Readiness プローブ
curl http://localhost:8080/q/health/ready
```

### 構造化ログ

サーバーは構造化ログをサポートし、設定可能なフォーマットを提供します。詳細は `application.properties` を参照してください。

---

## 🛠 開発

### コード品質ツール

このプロジェクトは複数の静的解析ツールを使用しています：

- **Checkstyle**: コードスタイルの強制
- **PMD**: コード品質解析
- **SpotBugs**: バグパターン検出

静的解析を実行：

```bash
./gradlew checkstyleMain pmdMain spotbugsMain
```

### 統合テスト

Testcontainers を使用した包括的な統合テスト：

```bash
./gradlew :integration-tests:test
```

統合テストのカバー範囲：
- ✅ ハッピーパス（アップロード、一覧、削除）
- ❌ エラーシナリオ（重複ファイル、サイズ超過、ファイル不在）
- 📊 メトリクスとヘルスエンドポイント
- 📖 API ドキュメントエンドポイント

### Docker Compose でのローカル開発

```bash
# 開発モードで全サービスを起動
docker-compose up

# コード変更後にリビルドして再起動
docker-compose up --build

# ログを表示
docker-compose logs -f

# 環境に対して統合テストを実行
./gradlew :integration-tests:test
```

---

## テスト

- **ユニットテスト**: JUnit 5 + Mockito
- **統合テスト**: Testcontainers + Docker Compose
- **コードカバレッジ**: Jacoco レポート、80% 閾値設定
  - テストレポートは _build/jacocoHtml/index.html_ に出力
- **継続的インテグレーション**: GitHub Actions による自動テスト

全てのテストを実行：

```bash
./gradlew clean test
```

カバレッジ付きで実行：

```bash
./gradlew clean build
# カバレッジレポートを表示: file-storage-server/build/jacocoHtml/index.html を開く
```

---

## 📚 API ドキュメント

サーバー起動後、インタラクティブな API ドキュメントが利用可能です：
- **Swagger UI**: http://localhost:8080/q/swagger-ui/
- **OpenAPI 仕様**: http://localhost:8080/q/openapi

---

## 🤝 コントリビューション

コントリビューションを歓迎します！お気軽にプルリクエストを提出してください。

1. リポジトリをフォーク
2. フィーチャーブランチを作成 (`git checkout -b feature/amazing-feature`)
3. 変更をコミット (`git commit -m 'Add some amazing feature'`)
4. ブランチにプッシュ (`git push origin feature/amazing-feature`)
5. プルリクエストを開く

---

## 📄 ライセンス

このプロジェクトは Apache License 2.0 の下でライセンスされています。詳細は LICENSE ファイルを参照してください。

---

## 🔄 継続的インテグレーション

- **ビルド**: 全てのプッシュで自動ビルド
- **テスト**: カバレッジレポート付きの包括的なテストスイート
- **セキュリティ**: セキュリティ脆弱性の CodeQL スキャン
- **依存関係**: Dependabot による自動依存関係更新
- **Docker**: Docker イメージの自動ビルドと公開
- **リリース**: 自動リリース作成とアーティファクト公開

---

**Quarkus と Java 21 で ❤️ を込めて構築**
