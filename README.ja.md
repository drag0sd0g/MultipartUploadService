[English](README.md)

# シンプルなファイルストレージサーバー&CLI

1. [前提条件](#前提条件)
2. [ビルドとパッケージ方法](#ビルドとパッケージ方法)
3. [サーバーの起動](#サーバーの起動)
   - [サーバーノート](#サーバーノート)
4. [クライアントの実行](#クライアントの実行)
   - [アップロード済みファイルの一覧](#アップロード済みファイルの一覧)
   - [ファイルをアップロード](#ファイルをアップロード)
   - [アップロード済みファイルを削除](#アップロード済みファイルを削除)
   - [クライアントノート](#クライアントノート)
5. [テストノート](#テスト)

---

## 前提条件

JDK 14 以降が必要です。JAVA_HOME を JDK インストール先に設定してください。

---

## ビルドとパッケージ方法

**file-storage** ルートディレクトリで次のコマンドを実行し、クライアント・サーバー両方をビルドします。

### Windows の場合

```bat
gradlew clean build distClient distServer
```

### Unix の場合

```bash
chmod +x gradlew && ./gradlew clean build distClient distServer
```

クライアントとサーバーの jar は _file-storage/build_ 配下の _fsclient/_ および _fsserver/_ フォルダに展開されます。

---

## サーバーの起動

_file-storage/build/fsserver_ に移動し、以下のコマンドでサーバーを起動します。

```shell script
java -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

プロパティは _src/main/resources_ 配下の _application.properties_ に設定されていますが、Quarkus の多数のデフォルト値も利用されます。**ポート**や**ホスト**を上書きしたい場合は
_jar_ 実行時に _-D_ 引数で指定してください。例 ↓

```shell script
java -Dquarkus.http.host="192.168.11.7" -Dquarkus.http.port=8085 -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

### サーバーノート

- デフォルトで http://127.0.0.1:8080 で起動し、 http://127.0.0.1:8080/q/swagger-ui/ にアクセスして
  正常起動および REST API ドキュメントと各エンドポイントのステータスコード説明を確認できます。
- Quarkus はマルチパートアップロードファイルを一時領域に保存し、その後*persistent*ストレージフォルダ（_data-server_）にコピーします。リクエスト処理後自動的に一時ファイルは削除されます。
- 全体の容量制限はありませんが、各ファイル 10MB のサイズ上限があります。
  この設定は _quarkus.http.limits.max-form-attribute-size_ で管理され、それを超過した場合は HTTP 413 となります。
  （CLI からも **GET** _/v1/stats/fileUploadSizeLimit_ でこの値を取得可能）
- REST API は現状 **/v1** バージョンです。

---

## クライアントの実行

別のコマンドラインで _file-storage/build/fsclient_ に移動し、以下の 3 通りからコマンドを選択します。

### アップロード済みファイルの一覧

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --list-files
```

または

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### ファイルをアップロード

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --upload-file <ファイルパス>
```

または

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -u <ファイルパス>
```

### アップロード済みファイルを削除

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --delete-file <ファイル名>
```

または

```shell script
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -d <ファイル名>
```

削除時はファイル名のみで OK。パス不要です。

デフォルトでは http://127.0.0.1:8080 のサーバーへ接続しますが、必要なら
CLI 起動時に _-Dfsserver.api.rootUrl_ で明示的に上書き可能です。例 ↓

```shell script
java -Dfsserver.api.rootUrl="http://192.168.11.7:8085" -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### クライアントノート

- コマンドラインオプション/引数が間違っている or 不足していれば、CLI は終了し、下記のような Usage ガイドを表示します:

```
usage: file-storage-client
To the usage command above, this CLI needs exactly one of the options:
 -d,--delete-file <arg>   指定したファイルをサーバーから削除（存在必須。なければエラー）
 -l,--list-files          アップロード済みファイル一覧表示（追加引数不要）
 -u,--upload-file <arg>   指定したファイルをアップロード。ローカルに存在・10MB以下必要
```

---

## テスト

- サーバー/CLI ともに Jacoco を使い 80%テストカバレッジを達成
  (テストレポートは _file-storage/file-storage-<server|client>/build/jacocoHtml/index.html_ に出力)
- macOS Ventura 13.1／Windows 11 21H2 で動作検証済
