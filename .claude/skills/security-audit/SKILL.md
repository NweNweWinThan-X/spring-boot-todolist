---
name: security-audit
description: セキュリティ脆弱性診断スキル。XSS、CSRF、アクセス制御不備、インジェクション、暗号化失敗、安全でないデシリアライゼーション、SSRF、シークレット管理、設定ミスを含むOWASP脆弱性を対象に、Spring Boot + Thymeleafプロジェクトを自動スキャンする。リスクレベルと推奨修正を含むレポートを生成する。
user-invocable: true
argument-hint: [ファイルパスまたはモジュール名（省略時はプロジェクト全体をスキャン）]
allowed-tools: Read, Grep, Glob, Edit, Write
---

# セキュリティ診断スキル

## 概要

このスキルは以下を組み合わせて実行します：
- **脆弱性チェック**: XSS（蓄積型）、XSS（DOM型）、Cookieセキュア属性、不正アクセス、個人情報漏洩、CSRF、クリックジャッキング、自動化対策（ログイン・フォーム）、Webコンポーネント、デフォルトサーバファイル、文字エンコーディング、Content-Typeエンコーディング、SQLインジェクション、暗号化失敗、安全でないデシリアライゼーション、SSRF、シークレット管理、入力検証戦略、セキュリティログ
- **Spring Boot + Thymeleaf + Spring Security + PostgreSQL** 固有のチェック

---

## 使用タイミング

- `/security-audit` を実行したとき
- 「セキュリティチェック」「脆弱性診断」「セキュリティ監査」と言われたとき
- 本番リリース前
- 新しいコントローラ・フォーム・APIエンドポイントを追加した後

---

## ワークフロー

1. **対象ファイルの特定**
   - 引数あり → 指定されたファイル／モジュールのみスキャン
   - 引数なし → プロジェクト全体をスキャン

2. **以下の順序でセキュリティチェックを実行：**

```
XSS（蓄積型）→ XSS（DOM型）→ Cookie／セッション
→ アクセス制御 → 個人情報 → CSRF → クリックジャッキング
→ 自動化対策 → Webコンポーネント
→ デフォルトファイル → 文字エンコーディング → Content-Type → SQLインジェクション
→ 暗号化失敗 → 安全でないデシリアライゼーション → SSRF
→ シークレット管理 → 入力検証戦略 → セキュリティログ
```

3. **各項目にリスクレベルを判定：**

| リスク | 判定基準 |
|--------|----------|
| 🔴 HIGH | 悪用可能な脆弱性が存在する |
| 🟡 MEDIUM | 対策が不完全または部分的に欠如している |
| 🟢 LOW | 適切に保護されている |
| ⚪ N/A | 関連するコードが見当たらない |

4. **全項目のレポートを出力** — リスクレベルと推奨修正を含む

---

## セキュリティチェック項目

---

### No.1 — XSS（蓄積型）

**概要：** ユーザー入力がDBに保存され、その後HTMLとしてレンダリングされることで、注入されたスクリプトが被害者のブラウザで実行される。

**チェック方法：**

```
STEP 1: ユーザー入力を受け取るフォームフィールドを特定
→ Grep: @RequestParam, @ModelAttribute, Command/Form/Requestクラス

STEP 2: それらの値をDBに保存するエンティティを特定
→ Grep: @Entity クラスの String フィールド

STEP 3: HTMLテンプレートでの値のレンダリング方法を確認
→ Glob: templates/**/*.html
→ th:utext が見つかった場合 → 🔴 HIGH
→ th:text / th:value のみの場合 → 🟢 LOW

STEP 4: 入力バリデーションの確認
→ @Pattern(regexp = "[^<>]*") が欠如している場合 → 🟡 MEDIUM
```

**安全な例 vs 危険な例：**
```html
<!-- 安全 — 出力を自動エスケープ -->
<span th:text="${dto.memo}"></span>

<!-- 危険 — HTMLをそのまま出力 -->
<span th:utext="${dto.memo}"></span>
```

**推奨修正：**
```java
// DTO／Commandクラスに追加
@Pattern(regexp = "[^<>\"'&]*", message = "HTMLタグは使用できません")
@Size(max = 20)
private String memo;
```

---

### No.2 — CookieセッションIDのSecure属性欠如

**概要：** Secure属性がない場合、セッションCookieが平文HTTP接続で盗まれる可能性がある。

**チェック方法：**
```
STEP 1: application.properties / application.yml を確認
→ server.servlet.session.cookie.secure=true が設定されているか

STEP 2: SecurityConfig.java を確認
→ CookieSameSiteSupplier / SameSite 設定が存在するか

STEP 3: 手動作成されたCookieを確認
→ Grep: new Cookie(...), response.addCookie(...)
→ cookie.setSecure(true) が欠如している場合 → 🔴 HIGH
```

**推奨修正：**
```properties
# application.properties
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=Lax
```

---

### No.3 — 権限のないユーザーによる機密データアクセス

**概要：** 適切な権限を持たないユーザーが、アクセスすべきでないデータを閲覧できる（アクセス制御の破綻）。

**チェック方法：**
```
STEP 1: コントローラの認可アノテーションを確認
→ Grep: @PreAuthorize, @Secured, hasRole(...)

STEP 2: Spring Securityの設定を確認
→ SecurityConfig の requestMatchers / antMatchers

STEP 3: サービス層でのユーザーID検証を確認
→ Grep: findById, getById
→ IDが認証済みユーザーのものか確認（水平権限昇格）
```

**推奨修正：**
```java
// コントローラ
@PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
@GetMapping("/deposit/detail/{id}")
public String detail(@PathVariable Long id, Authentication auth) {
  depositService.findByIdAndUser(id, auth.getName());
}

// サービス — 所有権を検証
public DepositDto findByIdAndUser(Long id, String username) {
  return repository.findByIdAndUsername(id, username)
      .orElseThrow(() -> new AppException(AppErrorCode.ACCESS_DENIED));
}
```

---

### No.4 — 不要な個人情報の出力

**概要：** レスポンスやログに不要な個人情報（メール、電話、住所）が含まれている。

**チェック方法：**
```
STEP 1: APIレスポンスのDTOを確認
→ Grep: Dto.java, @JsonProperty
→ 不要なフィールドに @JsonIgnore が欠如しているか

STEP 2: ログ出力を確認
→ Grep: log.info, log.debug, logger.info
→ 個人情報（氏名、メール、住所）がログに出力されているか

STEP 3: Thymeleafテンプレートを確認
→ 機密フィールドがhiddenフォームフィールドで公開されていないか
```

**推奨修正：**
```java
// 表示に必要なフィールドのみ含める — メール、電話、住所は除外
public record UserDto(
    Long id,
    String displayName
) {}

// ログ — IDのみ使用、個人情報は絶対に含めない
log.info("ユーザーログイン: userId={}", userId);
```

---

### No.5 — XSS（DOM型）

**概要：** JavaScriptがURLパラメータや `location.hash` から読み取り、サニタイズせずにDOMに直接書き込む。

**チェック方法：**
```
STEP 1: JavaScriptファイルを確認
→ Glob: src/main/resources/static/**/*.js
→ Glob: src/main/js/**/*.js (React)

STEP 2: 危険なパターンを検索
→ Grep: innerHTML, outerHTML, document.write, eval(...)
→ Grep: DOM書き込みで使用される location.href, location.hash, location.search

STEP 3: ThymeleafのインラインJavaScriptを確認
→ Grep: th:inline="javascript"
→ [[${...}]] の使用（Thymeleafは自動エスケープ — 安全）
```

**安全な例 vs 危険な例：**
```javascript
// 危険
document.getElementById('name').innerHTML = location.hash.substring(1);

// 安全
document.getElementById('name').textContent = location.hash.substring(1);
```

---

### No.6 — 不要なローカルリソースへのアクセス

**概要：** パストラバーサルや静的リソース設定ミスにより、非公開ファイルが公開される。

**チェック方法：**
```
STEP 1: ファイルアクセスコードを確認
→ Grep: new File(...), Paths.get(...), FileInputStream(...)
→ ユーザー入力がファイルパスに使用されているか

STEP 2: 静的リソースの設定を確認
→ SecurityConfig の resources().permitAll() のスコープ
→ src/main/resources/static/ の内容

STEP 3: 管理エンドポイントを確認
→ Grep: management.endpoints.web.exposure
→ 機密エンドポイントが公開されていないか
```

---

### No.7 — CSRF対策の欠如

**概要：** ログイン済みユーザーが意図しないリクエストを送信させられる。

**チェック方法：**
```
STEP 1: Spring SecurityのCSRF設定を確認
→ Grep: csrf().disable() または csrf(csrf -> csrf.disable())
→ 無効化されている場合 → 🔴 HIGH

STEP 2: フォームのCSRFトークンを確認
→ Glob: templates/**/*.html
→ Grep: _csrf, th:action
→ ThymeleafのみのCSRFトークンは th:action に自動含まれる

STEP 3: CSRF除外のスコープを確認
→ 除外が必要最小限に限定されているか
```

**推奨修正：**
```java
// SecurityConfig — CSRFを有効に保つ
http.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/webhook/**") // 必要な場合のみ除外
);
```

```html
<!-- Thymeleaf: th:action はCSRFトークンを自動的に含む -->
<form th:action="@{/deposit/memo}" method="post">
```

---

### No.8 — クリックジャッキング対策ヘッダーの未設定

**概要：** ページがiframeに埋め込まれ、ユーザーが意図しない操作を誘導される。

**チェック方法：**
```
STEP 1: SecurityConfigのセキュリティヘッダーを確認
→ Grep: frameOptions, X-Frame-Options, ContentSecurityPolicy

STEP 2: レスポンスヘッダーを確認
→ X-Frame-Options: DENY または SAMEORIGIN が設定されているか
→ Content-Security-Policy に frame-ancestors が含まれているか
```

**推奨修正：**
```java
// SecurityConfig
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; frame-ancestors 'none';")
    )
);
```

---

### No.9 — ログインURLの自動化対策の欠如

**概要：** レート制限やアカウントロックアウトがないため、ログインエンドポイントへのブルートフォース攻撃が可能になる。

**チェック方法：**
```
STEP 1: ログインエンドポイントのレート制限を確認
→ Grep: RateLimiter, Bucket4j, @RateLimited

STEP 2: アカウントロックアウトを確認
→ Grep: lockoutPolicy, failedAttempts, UserDetailsService

STEP 3: CAPTCHAを確認
→ Grep: captcha, recaptcha, hcaptcha
```

**推奨修正：**
```java
// 失敗回数をトラッキングし、閾値超過でアカウントをロック
@Service
public class LoginAttemptService {
  private static final int MAX_ATTEMPTS = 5;
  // 5回失敗でアカウントをロック
}
```

---

### No.10 — フォーム送信の自動化対策の欠如

**概要：** 登録フォームや申請フォームが自動化ボットに悪用される。

**チェック方法：**
```
STEP 1: 登録・申請フォームを確認
→ Glob: templates/register/**/*.html
→ CAPTCHAまたは送信レート制限が存在するか

STEP 2: バックエンドのレート制限を確認
→ Grep: RateLimiter, @RateLimited, throttle

STEP 3: ハニーポットフィールドを確認
→ ボット検出用の隠しフィールドが存在するか
```

---

### No.11 — 適切なセキュリティ設定のないWebコンポーネント

**概要：** 古いライブラリや脆弱なライブラリが既知のCVEを公開している。

**チェック方法：**
```
STEP 1: バックエンド依存関係を確認
→ Read: pom.xml または build.gradle
→ 古いライブラリバージョンが存在するか

STEP 2: フロントエンド依存関係を確認
→ Read: package.json
→ npm audit / yarn audit に相当する確認

STEP 3: Spring Bootのバージョンを確認
→ 現在のバージョンに既知のCVEが存在するか
```

**推奨修正：**
```xml
<!-- pom.xml — バージョンを明示的に管理し、最新に保つ -->
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version><!-- 最新の安定バージョンを使用 --></version>
</parent>
```

---

### No.12 — デフォルトサーバファイルの無効化の欠如

**概要：** デフォルトのエラーページやサーバ情報ヘッダーが実装の詳細を攻撃者に漏洩する。

**チェック方法：**
```
STEP 1: エラーページの設定を確認
→ Grep: ErrorController, /error, whitelabel
→ カスタムエラーページが存在するか

STEP 2: サーバ情報の公開を確認
→ Grep: server.error.include-stacktrace
→ server.error.include-message=never が設定されているか

STEP 3: Actuatorエンドポイントの公開を確認
→ Grep: management.endpoints.web.exposure.include
→ healthエンドポイントのみ公開されているか
```

**推奨修正：**
```properties
# application.properties
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-exception=false
management.endpoints.web.exposure.include=health
```

---

### No.13 — 文字エンコーディングが未定義のテキストレスポンス

**概要：** charsetが未定義のレスポンスはブラウザにエンコーディングを推測させ、XSSに悪用される可能性がある。

**チェック方法：**
```
STEP 1: コントローラの @ResponseBody メソッドを確認
→ Grep: @ResponseBody, ResponseEntity
→ produces = "text/plain" に charset=UTF-8 が欠如しているか

STEP 2: リクエストマッピングの produces 属性を確認
→ Grep: produces = "text/
→ charset なしの "text/html" または "text/plain" が存在するか
```

**推奨修正：**
```java
// 悪い例
@GetMapping(value = "/export", produces = "text/plain")

// 良い例
@GetMapping(value = "/export", produces = "text/plain;charset=UTF-8")
```

---

### No.14 — Content-Typeの文字エンコーディング欠如

**概要：** Content-Typeヘッダーにcharsetが欠如すると、エンコーディングベースのXSS攻撃が可能になる。

**チェック方法：**
```
STEP 1: デフォルトエンコーディングの設定を確認
→ Grep: CharacterEncodingFilter, spring.http.encoding
→ server.servlet.encoding.charset=UTF-8 が application.properties に設定されているか

STEP 2: 手動設定のContent-Typeヘッダーを確認
→ Grep: response.setContentType(...)
→ charset=UTF-8 が含まれているか

STEP 3: Thymeleafのエンコーディング設定を確認
→ spring.thymeleaf.encoding=UTF-8 が設定されているか
```

**推奨修正：**
```properties
# application.properties
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
spring.thymeleaf.encoding=UTF-8
```

---

### No.15 — SQLインジェクション（OWASP A03）

**概要：** ユーザー入力がSQLクエリに直接埋め込まれ、攻撃者がデータベースを操作できる。

**チェック方法：**
```
STEP 1: ネイティブクエリを確認
→ Grep: nativeQuery = true, @Query
→ 文字列連結でクエリを構築している場合 → 🔴 HIGH

STEP 2: JPQLクエリを確認
→ Grep: createQuery, EntityManager
→ 名前付きパラメータバインディングが使用されているか
```

**推奨修正：**
```java
// 悪い例 — SQLインジェクション脆弱性
@Query(value = "SELECT * FROM users WHERE name = '" + name + "'", nativeQuery = true)

// 良い例 — パラメータバインディング
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> findByName(@Param("name") String name);
```

---

### No.16 — 暗号化の失敗（OWASP A02）

**概要：** 弱いまたは欠如した暗号化により機密データが公開される — 弱いハッシュによるパスワード保存、ハードコードされた秘密情報、平文HTTP送信など。

**チェック方法：**
```
STEP 1: パスワードハッシュを確認
→ Grep: PasswordEncoder, BCrypt, Argon2
→ MD5, SHA1, ソルトなしSHA256 が見つかった場合 → 🔴 HIGH
→ BCryptPasswordEncoder の強度 < 10 の場合 → 🟡 MEDIUM

STEP 2: ハードコードされたシークレットを確認
→ Grep: password=, secret=, apiKey=, token= (.java / .properties ファイル内)
→ ソースコードにリテラルの認証情報 → 🔴 HIGH

STEP 3: HTTPSの強制を確認
→ Grep: requiresSecure, http.requiresChannel
→ HTTPトラフィックがHTTPSにリダイレクトされているか
```

**推奨修正：**
```java
// 良い例 — 強度12のBCrypt
@Bean
public PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder(12);
}

// 悪い例 — MD5やプレーンSHAは絶対に使用しない
MessageDigest.getInstance("MD5").digest(password.getBytes());
```

```properties
# application.properties — シークレットをソース管理ファイルに保存しない
# 環境変数またはシークレット管理ツールを使用する
spring.datasource.password=${DB_PASSWORD}
```

---

### No.17 — 安全でないデシリアライゼーション（OWASP A08）

**概要：** 信頼できないデータのデシリアライズにより、リモートコード実行やオブジェクトインジェクション攻撃が可能になる。

**チェック方法：**
```
STEP 1: Javaネイティブシリアライゼーションを確認
→ Grep: ObjectInputStream, readObject(), Serializable
→ 外部入力と共に使用されている場合 → 🔴 HIGH

STEP 2: Jacksonの設定を確認
→ Grep: ObjectMapper, enableDefaultTyping, @JsonTypeInfo
→ 型の許可リストなしの enableDefaultTyping() → 🔴 HIGH

STEP 3: 外部データのデシリアライゼーションを確認
→ Grep: @RequestBody, readValue(
→ 型が厳密に制御されているか
```

**推奨修正：**
```java
// 悪い例 — ポリモーフィック型攻撃に脆弱
objectMapper.enableDefaultTyping();

// 良い例 — 明示的な型バインディングのみ使用
objectMapper.activateDefaultTyping(
    LaissezFaireSubTypeValidator.instance,
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);

// 最良 — Javaシリアライゼーションを完全に避け、JSON/DTOマッピングを使用
```

---

### No.18 — SSRF — サーバーサイドリクエストフォージェリ（OWASP A10）

**概要：** サーバーがユーザーから提供されたURLを取得し、攻撃者が内部サービスやクラウドメタデータエンドポイントに到達できる。

**チェック方法：**
```
STEP 1: ユーザー提供のURLを使用した外部HTTPコールを確認
→ Grep: RestTemplate, WebClient, HttpURLConnection, URL(
→ URLの値が @RequestParam, @RequestBody, またはDBから取得されているか

STEP 2: URL許可リストの検証を確認
→ Grep: allowedHosts, allowedDomains, URI.getHost()
→ 許可リストが存在しない場合 → 🔴 HIGH

STEP 3: 内部IPブロッキングを確認
→ 127.0.0.1, 169.254.169.254（クラウドメタデータ）, 10.x, 192.168.x にアクセス可能か
```

**推奨修正：**
```java
// リクエスト前にURLを許可リストで検証する
private static final List<String> ALLOWED_HOSTS = List.of("api.example.com", "cdn.example.com");

public void validateUrl(String url) {
  URI uri = URI.create(url);
  if (!ALLOWED_HOSTS.contains(uri.getHost())) {
    throw new AppException(AppErrorCode.INVALID_REQUEST);
  }
}
```

---

### No.19 — シークレット管理

**概要：** APIキー、パスワード、トークンがソース管理にコミットされたり設定ファイルに埋め込まれたりすると、リポジトリアクセスで漏洩する可能性がある。

**チェック方法：**
```
STEP 1: application.properties / application.yml を確認
→ Grep: password=, secret=, key=, token=
→ リテラルの認証情報値（${ENV_VAR}参照でないもの）→ 🔴 HIGH

STEP 2: .gitignore を確認
→ application-prod.properties, .env, *.jks, *.p12 が除外されているか

STEP 3: Javaソースファイルのシークレットを確認
→ Grep: private static final String.*KEY =
→ Grep: private static final String.*SECRET =
→ ハードコードされたリテラル値 → 🔴 HIGH

STEP 4: 環境変数の使用を確認
→ すべての認証情報に ${...} プレースホルダーが使用されているか
```

**推奨修正：**
```properties
# 良い例 — 環境変数を参照する
spring.datasource.password=${DB_PASSWORD}
cloud.api.secret=${CLOUD_API_SECRET}
```

```
# .gitignore — 常に除外する
application-prod.properties
application-local.properties
.env
*.jks
*.p12
```

---

### No.20 — 入力検証戦略（許可リスト vs ブロックリスト）

**概要：** ブロックリストベースの検証（既知の悪いパターンをブロック）は簡単に回避される。許可リストベースの検証（既知の良いパターンのみ許可）が正しいアプローチである。

**チェック方法：**
```
STEP 1: DTO／Commandクラスのbean validationアノテーションを確認
→ Grep: @Pattern, @Size, @NotBlank, @Email
→ ブロックリスト正規表現を使用している場合（例：[^<>]）→ 🟡 MEDIUM
→ 許可リスト正規表現を使用している場合（例：[a-zA-Z0-9]）→ 🟢 LOW
→ バリデーションが全くない場合 → 🔴 HIGH

STEP 2: カスタムバリデータを確認
→ Grep: implements ConstraintValidator
→ 許可リストロジックが存在するか

STEP 3: コントローラのエントリポイントを確認
→ Grep: @RequestBody / @ModelAttribute に @Valid, @Validated
→ @Valid が欠如している場合 → 🔴 HIGH
```

**安全な例 vs 危険な例：**
```java
// ブロックリスト — 回避可能なため非推奨
@Pattern(regexp = "[^<>\"']*")

// 許可リスト — 安全な文字のみ許可するため推奨
@Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,@]+$", message = "使用できない文字が含まれています")
@Size(max = 100)
private String companyName;
```

```java
// コントローラのバウンダリで常にバリデーションを起動する
@PostMapping("/apply")
public String apply(@Valid @ModelAttribute ApplicationCommand cmd, BindingResult result) { }
```

---

### No.21 — セキュリティログ

**概要：** セキュリティイベントのログが欠如または不完全だと、攻撃の検出やインシデント調査が不可能になる。個人情報や認証情報をログに記録すると、別のデータ漏洩リスクが生じる。

**チェック方法：**
```
STEP 1: 認証イベントがログに記録されているか確認
→ Grep: AuthenticationSuccessHandler, AuthenticationFailureHandler
→ ログイン成功／失敗がuserIdとタイムスタンプ付きでログされているか

STEP 2: 認可失敗がログに記録されているか確認
→ Grep: AccessDeniedException, @ControllerAdvice
→ 403レスポンスがuserIdとリクエストパス付きでログされているか

STEP 3: ログへのPII／認証情報漏洩を確認
→ Grep: log.info, log.debug, log.error
→ email, password, phone, address, token がログ文字列に含まれている → 🔴 HIGH

STEP 4: ログレベルの設定を確認
→ Grep: logging.level in application.properties
→ 本番環境でDEBUGレベルが有効になっているか → 🟡 MEDIUM
```

**推奨修正：**
```java
// 良い例 — IDのみでセキュリティイベントをログに記録
log.warn("ログイン失敗: userId={}, ip={}", userId, clientIp);
log.info("アクセス拒否: userId={}, path={}", userId, requestPath);

// 悪い例 — 個人情報や認証情報を絶対にログに記録しない
log.info("ログイン試行: email={}, password={}", email, password); // 🔴
```

```properties
# application-prod.properties
logging.level.root=WARN
logging.level.com.example.security=INFO
```

---

## レポート出力フォーマット

スキャン後、以下の形式でレポートを出力する：

```
## セキュリティ診断レポート
日付: YYYY-MM-DD
スコープ: [ファイル名 または "プロジェクト全体"]

| No. | 脆弱性 | リスク | ファイル | 詳細 |
|-----|--------|--------|----------|------|
| 1 | XSS（蓄積型） | 🟢 LOW | templates/deposit/... | th:text を使用 — 安全 |
| 2 | Cookieセキュア属性 | 🟡 MEDIUM | application.properties | secure=true 未設定 |
| 3 | 不正アクセス | 🟢 LOW | SecurityConfig.java | @PreAuthorize が設定済み |
| 4 | 個人情報の不要な出力 | ... | ... | ... |
| 5 | XSS（DOM型） | ... | ... | ... |
| 6 | ローカルリソース公開 | ... | ... | ... |
| 7 | CSRF対策 | ... | ... | ... |
| 8 | クリックジャッキング | ... | ... | ... |
| 9 | ログイン自動化対策 | ... | ... | ... |
| 10 | フォーム自動化対策 | ... | ... | ... |
| 11 | Webコンポーネント | ... | ... | ... |
| 12 | デフォルトサーバファイル | ... | ... | ... |
| 13 | 文字エンコーディング | ... | ... | ... |
| 14 | Content-Typeエンコーディング | ... | ... | ... |
| 15 | SQLインジェクション | ... | ... | ... |
| 16 | 暗号化の失敗 | ... | ... | ... |
| 17 | 安全でないデシリアライゼーション | ... | ... | ... |
| 18 | SSRF | ... | ... | ... |
| 19 | シークレット管理 | ... | ... | ... |
| 20 | 入力検証戦略 | ... | ... | ... |
| 21 | セキュリティログ | ... | ... | ... |

## 推奨修正
🔴 HIGH を優先順位の高い順に、コード例付きでリストアップする。
```

---

## プロジェクト固有のチェックポイント

| レイヤー | チェック内容 |
|----------|------------|
| **Thymeleaf** | `th:utext` を使用していないか；`th:action` がCSRFトークンを自動的に含むか |
| **Spring Security** | `csrf().disable()` がないか；`frameOptions` が設定済みか；BCryptの強度 ≥ 12か |
| **Spring Data JPA** | ネイティブクエリで名前付きパラメータバインディングを使用しているか；JPQLで文字列連結がないか |
| **application.properties** | エンコーディング、エラー、Cookie、Actuatorの設定；ハードコードされた認証情報がないか |
| **React（Vite）** | `dangerouslySetInnerHTML` がないか；生の `innerHTML` 書き込みがないか |
| **Beanバリデーション** | `@Pattern`（許可リスト）、`@Size`、`@Valid` がすべてのユーザー入力エントリポイントに適用されているか |
| **ObjectMapper / Jackson** | `enableDefaultTyping()` がないか；明示的な型バインディングのみを使用しているか |
| **外部HTTPコール** | URL許可リストが適用されているか；ユーザー制御URLがRestTemplate/WebClientに渡されていないか |
| **シークレット** | すべての認証情報が `${ENV_VAR}` 経由か；機密ファイルが `.gitignore` に含まれているか |
| **ログ** | 認証／認可イベントがログに記録されているか；ログ文字列にPIIや認証情報がゼロか |
